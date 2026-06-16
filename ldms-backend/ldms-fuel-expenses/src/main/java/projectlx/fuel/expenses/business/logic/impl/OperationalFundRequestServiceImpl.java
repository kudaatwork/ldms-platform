package projectlx.fuel.expenses.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fuel.expenses.business.auditable.api.OperationalFundRequestServiceAuditable;
import projectlx.fuel.expenses.business.logic.api.FuelTelemetryLogService;
import projectlx.fuel.expenses.business.logic.api.OperationalFundRequestService;
import projectlx.fuel.expenses.business.logic.support.CallerOrganizationResolver;
import projectlx.fuel.expenses.business.logic.support.FuelExpensesMapper;
import projectlx.fuel.expenses.business.logic.support.FundRequestNumberGenerator;
import projectlx.fuel.expenses.business.validator.api.OperationalFundRequestServiceValidator;
import projectlx.fuel.expenses.clients.TripTrackingServiceClient;
import projectlx.fuel.expenses.model.FuelSession;
import projectlx.fuel.expenses.model.OperationalFundRequest;
import projectlx.fuel.expenses.repository.OperationalFundRequestRepository;
import projectlx.fuel.expenses.utils.dtos.OperationalFundRequestDto;
import projectlx.fuel.expenses.utils.enums.FundRequestStatus;
import projectlx.fuel.expenses.utils.enums.FundRequestType;
import projectlx.fuel.expenses.utils.enums.FuelSessionStatus;
import projectlx.fuel.expenses.utils.enums.I18Code;
import projectlx.fuel.expenses.utils.requests.ApproveFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CancelFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.CreateFundRequestRequest;
import projectlx.fuel.expenses.utils.requests.FundRequestFilterRequest;
import projectlx.fuel.expenses.utils.requests.RecordTripEventFeignRequest;
import projectlx.fuel.expenses.utils.requests.RejectFundRequestRequest;
import projectlx.fuel.expenses.utils.responses.OperationalFundRequestResponse;
import projectlx.fuel.expenses.repository.FuelSessionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Operational Fund Request business logic.
 *
 * Workflow: PENDING → APPROVED | REJECTED | CANCELLED
 *
 * On FUEL_TOP_UP approval:
 *   1. Apply approved litres to the active FuelSession (capped at tank capacity).
 *   2. Log a TOP_UP telemetry entry.
 *   3. Publish fund_request.approved event.
 *
 * On FUNDS approval:
 *   1. Publish fund_request.approved event with approved amount for billing reconciliation.
 *
 * Note: No @Service — wired in BusinessConfig.
 */
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OperationalFundRequestServiceImpl implements OperationalFundRequestService {

    private static final String FUEL_EXCHANGE                      = "fuel.exchange";
    private static final String ROUTING_KEY_FUND_REQUEST_CREATED   = "fund_request.created";
    private static final String ROUTING_KEY_FUND_REQUEST_APPROVED  = "fund_request.approved";
    private static final String ROUTING_KEY_FUND_REQUEST_REJECTED  = "fund_request.rejected";

    private final OperationalFundRequestServiceValidator  validator;
    private final OperationalFundRequestServiceAuditable  auditable;
    private final OperationalFundRequestRepository        operationalFundRequestRepository;
    private final FuelSessionRepository                   fuelSessionRepository;
    private final FuelTelemetryLogService                 fuelTelemetryLogService;
    private final CallerOrganizationResolver              callerOrganizationResolver;
    private final FundRequestNumberGenerator              fundRequestNumberGenerator;
    private final FuelExpensesMapper                      fuelExpensesMapper;
    private final RabbitTemplate                          rabbitTemplate;
    private final MessageService                          messageService;
    private final TripTrackingServiceClient               tripTrackingServiceClient;

    // ============================================================
    // CREATE
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperationalFundRequestResponse create(CreateFundRequestRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = validator.isCreateFundRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return buildErrorResponse(400, validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Resolve caller organisation
        // ============================================================
        Long organizationId = callerOrganizationResolver.resolveCallerOrganizationId(username);

        // ============================================================
        // STEP 3: Build and persist the fund request
        // ============================================================
        OperationalFundRequest entity = new OperationalFundRequest();
        entity.setRequestNumber(fundRequestNumberGenerator.generate());
        entity.setTripId(request.getTripId());
        entity.setOrganizationId(organizationId != null ? organizationId : 0L);
        entity.setFleetDriverId(request.getFleetDriverId());
        entity.setFleetAssetId(request.getFleetAssetId());
        entity.setRequestType(request.getRequestType());
        entity.setStatus(FundRequestStatus.PENDING);
        entity.setLitersRequested(request.getLitersRequested());
        entity.setAmountRequested(request.getAmountRequested());
        entity.setCurrencyCode(request.getCurrencyCode());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setDriverNotes(request.getDriverNotes());
        entity.setEntityStatus(EntityStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(username);

        OperationalFundRequest saved = auditable.create(entity, locale, username);
        log.info("OperationalFundRequest created: id={} number={} type={} tripId={}",
                saved.getId(), saved.getRequestNumber(), saved.getRequestType(), saved.getTripId());

        // ============================================================
        // STEP 4: Publish fund_request.created
        // ============================================================
        publishEvent(ROUTING_KEY_FUND_REQUEST_CREATED, buildCreatedEventPayload(saved));

        OperationalFundRequestResponse response = new OperationalFundRequestResponse();
        response.setSuccess(true);
        response.setStatusCode(201);
        response.setOperationalFundRequestDto(fuelExpensesMapper.toDto(saved));
        return response;
    }

    // ============================================================
    // APPROVE
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperationalFundRequestResponse approve(ApproveFundRequestRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = validator.isApproveFundRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return buildErrorResponse(400, validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load with pessimistic lock
        // ============================================================
        Optional<OperationalFundRequest> entityOpt =
                operationalFundRequestRepository.findByIdAndEntityStatusNot(request.getRequestId(),
                        EntityStatus.DELETED);
        if (entityOpt.isEmpty()) {
            return buildErrorResponse(404, List.of(
                    messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_NOT_FOUND.getCode(),
                            new String[]{String.valueOf(request.getRequestId())}, locale)));
        }

        OperationalFundRequest entity = entityOpt.get();

        // ============================================================
        // STEP 3: Guard — only PENDING requests can be approved
        // ============================================================
        if (entity.getStatus() != FundRequestStatus.PENDING) {
            return buildErrorResponse(409, List.of(
                    messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_NOT_PENDING.getCode(),
                            new String[]{entity.getStatus().name()}, locale)));
        }

        // ============================================================
        // STEP 4: Validate approval amounts by request type
        // FUEL_TOP_UP requires approved litres; FUNDS requires approved amount.
        // MECHANIC requires neither — only dispatcher confirmation.
        // ============================================================
        if (entity.getRequestType() == FundRequestType.FUEL_TOP_UP) {
            if (request.getApprovedLiters() == null || request.getApprovedLiters().compareTo(BigDecimal.ZERO) <= 0) {
                return buildErrorResponse(400, List.of(
                        messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_APPROVED_LITERS_REQUIRED.getCode(),
                                new String[]{}, locale)));
            }
        }
        if (entity.getRequestType() == FundRequestType.FUNDS) {
            if (request.getApprovedAmount() == null || request.getApprovedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return buildErrorResponse(400, List.of(
                        messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_APPROVED_AMOUNT_REQUIRED.getCode(),
                                new String[]{}, locale)));
            }
        }

        // ============================================================
        // STEP 5: Apply decision
        // ============================================================
        entity.setStatus(FundRequestStatus.APPROVED);
        entity.setApprovedLiters(request.getApprovedLiters());
        entity.setApprovedAmount(request.getApprovedAmount());
        entity.setDecidedBy(username);
        entity.setDecidedAt(LocalDateTime.now());
        entity.setModifiedAt(LocalDateTime.now());
        entity.setModifiedBy(username);

        OperationalFundRequest saved = auditable.update(entity, locale, username);
        log.info("OperationalFundRequest approved: id={} type={} by={}", saved.getId(), saved.getRequestType(), username);

        // ============================================================
        // STEP 6a: FUEL_TOP_UP → apply litres to active FuelSession + record roadside stop
        // ============================================================
        if (entity.getRequestType() == FundRequestType.FUEL_TOP_UP) {
            applyTopUpToFuelSession(saved, locale, username);
            recordRoadsideEventOnTrip(saved.getTripId(), "ROADSIDE_FUEL_STOP",
                    "Fuel top-up approved: " + saved.getApprovedLiters() + " litres", locale);
        }

        // ============================================================
        // STEP 6b: MECHANIC → record roadside mechanic stop on trip
        // ============================================================
        if (entity.getRequestType() == FundRequestType.MECHANIC) {
            recordRoadsideEventOnTrip(saved.getTripId(), "ROADSIDE_MECHANIC_STOP",
                    "Mechanic request approved by " + username, locale);
        }

        // ============================================================
        // STEP 6c: Publish fund_request.approved
        // ============================================================
        publishEvent(ROUTING_KEY_FUND_REQUEST_APPROVED, buildDecisionEventPayload(saved));

        OperationalFundRequestResponse response = new OperationalFundRequestResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setOperationalFundRequestDto(fuelExpensesMapper.toDto(saved));
        return response;
    }

    // ============================================================
    // REJECT
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperationalFundRequestResponse reject(RejectFundRequestRequest request, Locale locale, String username) {

        ValidatorDto validation = validator.isRejectFundRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return buildErrorResponse(400, validation.getErrorMessages());
        }

        Optional<OperationalFundRequest> entityOpt =
                operationalFundRequestRepository.findByIdAndEntityStatusNot(request.getRequestId(),
                        EntityStatus.DELETED);
        if (entityOpt.isEmpty()) {
            return buildErrorResponse(404, List.of(
                    messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_NOT_FOUND.getCode(),
                            new String[]{String.valueOf(request.getRequestId())}, locale)));
        }

        OperationalFundRequest entity = entityOpt.get();

        if (entity.getStatus() != FundRequestStatus.PENDING) {
            return buildErrorResponse(409, List.of(
                    messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_NOT_PENDING.getCode(),
                            new String[]{entity.getStatus().name()}, locale)));
        }

        entity.setStatus(FundRequestStatus.REJECTED);
        entity.setRejectionReason(request.getRejectionReason());
        entity.setDecidedBy(username);
        entity.setDecidedAt(LocalDateTime.now());
        entity.setModifiedAt(LocalDateTime.now());
        entity.setModifiedBy(username);

        OperationalFundRequest saved = auditable.update(entity, locale, username);
        log.info("OperationalFundRequest rejected: id={} by={}", saved.getId(), username);

        publishEvent(ROUTING_KEY_FUND_REQUEST_REJECTED, buildDecisionEventPayload(saved));

        OperationalFundRequestResponse response = new OperationalFundRequestResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setOperationalFundRequestDto(fuelExpensesMapper.toDto(saved));
        return response;
    }

    // ============================================================
    // CANCEL
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperationalFundRequestResponse cancel(CancelFundRequestRequest request, Locale locale, String username) {

        ValidatorDto validation = validator.isCancelFundRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return buildErrorResponse(400, validation.getErrorMessages());
        }

        Optional<OperationalFundRequest> entityOpt =
                operationalFundRequestRepository.findByIdAndEntityStatusNot(request.getRequestId(),
                        EntityStatus.DELETED);
        if (entityOpt.isEmpty()) {
            return buildErrorResponse(404, List.of(
                    messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_NOT_FOUND.getCode(),
                            new String[]{String.valueOf(request.getRequestId())}, locale)));
        }

        OperationalFundRequest entity = entityOpt.get();

        if (entity.getStatus() != FundRequestStatus.PENDING) {
            return buildErrorResponse(409, List.of(
                    messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_NOT_PENDING.getCode(),
                            new String[]{entity.getStatus().name()}, locale)));
        }

        entity.setStatus(FundRequestStatus.CANCELLED);
        if (request.getCancellationReason() != null && !request.getCancellationReason().isBlank()) {
            entity.setRejectionReason(request.getCancellationReason());
        }
        entity.setDecidedBy(username);
        entity.setDecidedAt(LocalDateTime.now());
        entity.setModifiedAt(LocalDateTime.now());
        entity.setModifiedBy(username);

        OperationalFundRequest saved = auditable.update(entity, locale, username);
        log.info("OperationalFundRequest cancelled: id={} by={}", saved.getId(), username);

        OperationalFundRequestResponse response = new OperationalFundRequestResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setOperationalFundRequestDto(fuelExpensesMapper.toDto(saved));
        return response;
    }

    // ============================================================
    // FIND BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public OperationalFundRequestResponse findById(Long id, Locale locale, String username) {

        ValidatorDto validation = validator.isFindByIdRequestValid(id, locale);
        if (!validation.getSuccess()) {
            return buildErrorResponse(400, validation.getErrorMessages());
        }

        Optional<OperationalFundRequest> entityOpt =
                operationalFundRequestRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (entityOpt.isEmpty()) {
            return buildErrorResponse(404, List.of(
                    messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_NOT_FOUND.getCode(),
                            new String[]{String.valueOf(id)}, locale)));
        }

        OperationalFundRequestResponse response = new OperationalFundRequestResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setOperationalFundRequestDto(fuelExpensesMapper.toDto(entityOpt.get()));
        return response;
    }

    // ============================================================
    // FIND BY MULTIPLE FILTERS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public OperationalFundRequestResponse findByMultipleFilters(FundRequestFilterRequest request, Locale locale,
            String username) {

        ValidatorDto validation = validator.isFindByMultipleFiltersRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return buildErrorResponse(400, validation.getErrorMessages());
        }

        Specification<OperationalFundRequest> spec = buildSpecification(request);
        PageRequest pageable = PageRequest.of(
                request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OperationalFundRequest> resultPage =
                operationalFundRequestRepository.findAll(spec, pageable);

        List<OperationalFundRequestDto> dtoList = resultPage.getContent().stream()
                .map(fuelExpensesMapper::toDto)
                .collect(Collectors.toList());

        OperationalFundRequestResponse response = new OperationalFundRequestResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setOperationalFundRequestDtoList(dtoList);
        response.setTotalElements(resultPage.getTotalElements());
        response.setTotalPages(resultPage.getTotalPages());
        return response;
    }

    // ============================================================
    // FUEL TOP-UP HELPER — apply litres to active FuelSession
    // ============================================================

    private void applyTopUpToFuelSession(OperationalFundRequest fundRequest, Locale locale, String username) {
        try {
            Optional<FuelSession> sessionOpt = fuelSessionRepository
                    .findByTripIdAndStatusAndEntityStatusNot(fundRequest.getTripId(), FuelSessionStatus.ACTIVE,
                            EntityStatus.DELETED);

            if (sessionOpt.isEmpty()) {
                log.warn("No active FuelSession found for tripId={} — top-up skipped", fundRequest.getTripId());
                return;
            }

            FuelSession session = sessionOpt.get();
            BigDecimal approvedLiters = fundRequest.getApprovedLiters();

            // Cap at tank capacity to prevent overfill.
            BigDecimal newRemaining = session.getFuelRemainingLiters().add(approvedLiters);
            if (newRemaining.compareTo(session.getTankCapacityLiters()) > 0) {
                newRemaining = session.getTankCapacityLiters();
                log.info("Top-up capped at tank capacity for tripId={}", fundRequest.getTripId());
            }

            BigDecimal newPct = newRemaining
                    .divide(session.getTankCapacityLiters(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);

            session.setFuelRemainingLiters(newRemaining.setScale(2, RoundingMode.HALF_UP));
            session.setFuelLevelPct(newPct);
            session.setModifiedAt(LocalDateTime.now());
            session.setModifiedBy(username);
            fuelSessionRepository.save(session);
            log.info("FuelSession updated after top-up: tripId={} newLevel={}%", fundRequest.getTripId(), newPct);

            // Log the TOP_UP telemetry entry.
            fuelTelemetryLogService.logTopUp(
                    fundRequest.getTripId(),
                    session.getOrganizationId(),
                    session.getFleetAssetId(),
                    session.getId(),
                    approvedLiters,
                    newPct,
                    newRemaining);

        } catch (Exception ex) {
            log.error("Error applying top-up to FuelSession for tripId={}: {}",
                    fundRequest.getTripId(), ex.getMessage(), ex);
        }
    }

    // ============================================================
    // SPECIFICATION — multi-filter query
    // ============================================================

    private Specification<OperationalFundRequest> buildSpecification(FundRequestFilterRequest filter) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            predicates.add(cb.notEqual(root.get("entityStatus"), EntityStatus.DELETED));

            if (filter.getTripId() != null) {
                predicates.add(cb.equal(root.get("tripId"), filter.getTripId()));
            }
            if (filter.getOrganizationId() != null) {
                predicates.add(cb.equal(root.get("organizationId"), filter.getOrganizationId()));
            }
            if (filter.getFleetDriverId() != null) {
                predicates.add(cb.equal(root.get("fleetDriverId"), filter.getFleetDriverId()));
            }
            if (filter.getFleetAssetId() != null) {
                predicates.add(cb.equal(root.get("fleetAssetId"), filter.getFleetAssetId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getRequestType() != null) {
                predicates.add(cb.equal(root.get("requestType"), filter.getRequestType()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void publishEvent(String routingKey, Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(FUEL_EXCHANGE, routingKey, payload);
            log.info("Published {} event for requestId={}", routingKey, payload.get("requestId"));
        } catch (Exception ex) {
            log.error("Failed to publish {} event: {}", routingKey, ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildCreatedEventPayload(OperationalFundRequest entity) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId",     entity.getId());
        payload.put("requestNumber", entity.getRequestNumber());
        payload.put("tripId",        entity.getTripId());
        payload.put("organizationId",entity.getOrganizationId());
        payload.put("fleetDriverId", entity.getFleetDriverId());
        payload.put("requestType",   entity.getRequestType().name());
        payload.put("status",        entity.getStatus().name());
        return payload;
    }

    private Map<String, Object> buildDecisionEventPayload(OperationalFundRequest entity) {
        Map<String, Object> payload = buildCreatedEventPayload(entity);
        payload.put("approvedLiters",  entity.getApprovedLiters());
        payload.put("approvedAmount",  entity.getApprovedAmount());
        payload.put("rejectionReason", entity.getRejectionReason());
        payload.put("decidedBy",       entity.getDecidedBy());
        return payload;
    }

    // ============================================================
    // COMPLETE ROADSIDE STOP
    // ============================================================

    /**
     * Records a ROADSIDE_RESUMED event on the trip to transition it from ROADSIDE_HOLD → IN_TRANSIT.
     * Called by the driver/fleet manager when the mechanic work or fuel fill is complete.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperationalFundRequestResponse completeRoadsideStop(Long tripId, Locale locale, String username) {

        if (tripId == null || tripId <= 0) {
            return buildErrorResponse(400, List.of(
                    messageService.getMessage(I18Code.MESSAGE_FUND_REQUEST_NOT_FOUND.getCode(),
                            new String[]{"tripId is required"}, locale)));
        }

        recordRoadsideEventOnTrip(tripId, "ROADSIDE_RESUMED",
                "Roadside stop completed by " + username, locale);
        log.info("Roadside stop completed for tripId={} by user={}", tripId, username);

        OperationalFundRequestResponse response = new OperationalFundRequestResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        return response;
    }

    // ============================================================
    // ROADSIDE TRIP EVENT HELPER
    // ============================================================

    /**
     * Posts a roadside-related trip event to ldms-trip-tracking via Feign (non-blocking — errors are swallowed).
     */
    private void recordRoadsideEventOnTrip(Long tripId, String eventType, String notes, Locale locale) {
        if (tripId == null) {
            log.warn("recordRoadsideEventOnTrip: tripId is null — skipping {} event", eventType);
            return;
        }
        try {
            RecordTripEventFeignRequest feignRequest = new RecordTripEventFeignRequest();
            feignRequest.setTripId(tripId);
            feignRequest.setEventType(eventType);
            feignRequest.setNotes(notes);
            tripTrackingServiceClient.recordTripEvent(feignRequest, locale);
            log.info("Recorded {} event on tripId={}", eventType, tripId);
        } catch (Exception ex) {
            log.error("Failed to record {} event on tripId={}: {}", eventType, tripId, ex.getMessage());
        }
    }

    private OperationalFundRequestResponse buildErrorResponse(int statusCode, List<String> errors) {
        OperationalFundRequestResponse response = new OperationalFundRequestResponse();
        response.setSuccess(false);
        response.setStatusCode(statusCode);
        response.setErrorMessages(errors);
        return response;
    }
}
