package projectlx.trip.tracking.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.trip.tracking.business.auditable.api.DeliveryOtpServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripEventServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripServiceAuditable;
import projectlx.trip.tracking.business.logic.api.TripService;
import projectlx.trip.tracking.business.logic.support.CallerOrganizationResolver;
import projectlx.trip.tracking.business.logic.support.TripMapper;
import projectlx.trip.tracking.business.logic.support.TripNumberGenerator;
import projectlx.trip.tracking.business.validator.api.TripServiceValidator;
import projectlx.trip.tracking.clients.InventoryManagementServiceClient;
import projectlx.trip.tracking.clients.ShipmentManagementServiceClient;
import projectlx.trip.tracking.model.DeliveryOtp;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripEvent;
import projectlx.trip.tracking.repository.DeliveryOtpRepository;
import projectlx.trip.tracking.repository.TripEventRepository;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.utils.config.RabbitMQProducerConfig;
import projectlx.trip.tracking.utils.dtos.InventoryCompleteWithGrvDto;
import projectlx.trip.tracking.utils.dtos.InventoryStartTransitDto;
import projectlx.trip.tracking.utils.dtos.ShipmentSummaryDto;
import projectlx.trip.tracking.utils.requests.UpdateShipmentStatusFeignRequest;
import projectlx.trip.tracking.utils.responses.ShipmentFeignResponse;
import projectlx.trip.tracking.utils.dtos.TripDto;
import projectlx.trip.tracking.utils.dtos.TripEventDto;
import projectlx.trip.tracking.utils.enums.I18Code;
import projectlx.trip.tracking.utils.enums.TripEventType;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.trip.tracking.utils.requests.RecordLocationRequest;
import projectlx.trip.tracking.utils.requests.RecordTripEventRequest;
import projectlx.trip.tracking.utils.requests.StartTripRequest;
import projectlx.trip.tracking.utils.requests.TriggerArrivalRequest;
import projectlx.trip.tracking.utils.requests.TripFilterRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.trip.tracking.utils.responses.TripResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class TripServiceImpl implements TripService {

    private final TripServiceValidator tripServiceValidator;
    private final TripServiceAuditable tripServiceAuditable;
    private final TripEventServiceAuditable tripEventServiceAuditable;
    private final DeliveryOtpServiceAuditable deliveryOtpServiceAuditable;
    private final TripRepository tripRepository;
    private final TripEventRepository tripEventRepository;
    private final DeliveryOtpRepository deliveryOtpRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final TripNumberGenerator tripNumberGenerator;
    private final ShipmentManagementServiceClient shipmentManagementServiceClient;
    private final InventoryManagementServiceClient inventoryManagementServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final MessageService messageService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private static final List<TripStatus> ACTIVE_STATUSES = List.of(
            TripStatus.SCHEDULED, TripStatus.IN_TRANSIT, TripStatus.ARRIVED, TripStatus.OTP_PENDING);

    /**
     * Start Trip: Allocate driver + asset, link inventory transfer, notify shipment service.
     *
     * Flow:
     * 1. Validate request
     * 2. Resolve caller organisation
     * 3. Verify no active trip already exists for shipment
     * 4. Fetch shipment from shipment-management; assert status is ALLOCATED
     * 5. Create trip record (SCHEDULED → IN_TRANSIT)
     * 6. Call inventory start-transit for linked transfer
     * 7. Update shipment status to IN_TRANSIT
     * 8. Record DEPARTED event
     * 9. Publish trip.started
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripResponse startTrip(StartTripRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = tripServiceValidator.isStartTripRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_TRIP_START_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Resolve caller organisation
        // ============================================================
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Guard — ensure no active trip for the same shipment
        // ============================================================
        boolean hasActiveTrip = tripRepository.existsByShipmentIdAndStatusNotInAndEntityStatusNot(
                request.getShipmentId(), List.of(TripStatus.DELIVERED, TripStatus.CANCELLED), EntityStatus.DELETED);
        if (hasActiveTrip) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_ALREADY_ACTIVE.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 4: Fetch shipment details and validate ALLOCATED status
        // ============================================================
        ShipmentSummaryDto shipment = null;
        try {
            ShipmentFeignResponse shipmentResponse =
                    shipmentManagementServiceClient.findShipmentById(request.getShipmentId(), locale);
            if (shipmentResponse != null && shipmentResponse.isSuccess()) {
                shipment = shipmentResponse.getShipmentDto();
            }
        } catch (Exception ex) {
            log.warn("Could not fetch shipment {} from shipment-management: {}", request.getShipmentId(), ex.getMessage());
        }
        if (shipment == null || !"ALLOCATED".equalsIgnoreCase(shipment.getStatus())) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_SHIPMENT_NOT_ALLOCATED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 5: Persist the trip record
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        Trip trip = new Trip();
        trip.setTripNumber(tripNumberGenerator.generate());
        trip.setOrganizationId(organizationId);
        trip.setShipmentId(request.getShipmentId());
        trip.setInventoryTransferId(shipment.getInventoryTransferId());
        trip.setFleetDriverId(request.getFleetDriverId());
        trip.setFleetAssetId(request.getFleetAssetId());
        trip.setStatus(TripStatus.IN_TRANSIT);
        trip.setStartedAt(now);
        trip.setFromWarehouseName(shipment.getFromWarehouseName());
        trip.setToWarehouseName(shipment.getToWarehouseName());
        trip.setProductName(shipment.getProductName());
        trip.setEntityStatus(EntityStatus.ACTIVE);
        trip.setCreatedAt(now);
        trip.setCreatedBy(username);

        Trip saved = tripServiceAuditable.create(trip, locale, username);
        log.info("Trip created: id={} number={} shipment={} org={}",
                saved.getId(), saved.getTripNumber(), saved.getShipmentId(), organizationId);

        // ============================================================
        // STEP 6: Start inventory transit for linked transfer
        // ============================================================
        if (saved.getInventoryTransferId() != null) {
            try {
                InventoryStartTransitDto startTransitDto = new InventoryStartTransitDto();
                startTransitDto.setTransferId(saved.getInventoryTransferId());
                startTransitDto.setStartedByUserId(request.getStartedByUserId());
                inventoryManagementServiceClient.startTransit(startTransitDto, locale);
                log.info("Inventory transfer {} started for trip {}", saved.getInventoryTransferId(), saved.getId());
            } catch (Exception ex) {
                log.error("Failed to start inventory transit for transfer {}: {}", saved.getInventoryTransferId(), ex.getMessage());
            }
        }

        // ============================================================
        // STEP 7: Update shipment status → IN_TRANSIT
        // ============================================================
        try {
            UpdateShipmentStatusFeignRequest statusUpdate = new UpdateShipmentStatusFeignRequest();
            statusUpdate.setShipmentId(saved.getShipmentId());
            statusUpdate.setStatus("IN_TRANSIT");
            statusUpdate.setTripId(saved.getId());
            shipmentManagementServiceClient.updateShipmentStatus(statusUpdate, locale);
        } catch (Exception ex) {
            log.error("Failed to update shipment {} to IN_TRANSIT: {}", saved.getShipmentId(), ex.getMessage());
        }

        // ============================================================
        // STEP 8: Record DEPARTED event
        // ============================================================
        recordTripEvent(saved, TripEventType.DEPARTED, null, null,
                "Trip started by " + username, request.getStartedByUserId(), username, now, locale);

        // ============================================================
        // STEP 9: Publish trip.started event
        // ============================================================
        publishTripEvent(RabbitMQProducerConfig.ROUTING_KEY_TRIP_STARTED, saved);

        TripResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_TRIP_START_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDto(TripMapper.toDto(saved));
        return response;
    }

    /**
     * Record an arbitrary trip event (checkpoint, border crossing, note, etc.)
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripResponse recordEvent(RecordTripEventRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = tripServiceValidator.isRecordTripEventRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_TRIP_EVENT_RECORD_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load trip (must be in transit or arrived)
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(request.getTripId(), EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Persist event
        // ============================================================
        TripEventType eventType = TripEventType.valueOf(request.getEventType().trim().toUpperCase());
        TripEvent event = recordTripEvent(trip, eventType, request.getLatitude(), request.getLongitude(),
                request.getNotes(), null, username, LocalDateTime.now(), locale);

        // ============================================================
        // STEP 4: Publish trip.event_recorded
        // ============================================================
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tripId", trip.getId());
            payload.put("tripNumber", trip.getTripNumber());
            payload.put("eventType", eventType.name());
            rabbitTemplate.convertAndSend(RabbitMQProducerConfig.TRIP_EXCHANGE,
                    RabbitMQProducerConfig.ROUTING_KEY_TRIP_EVENT_RECORDED, payload);
        } catch (Exception ex) {
            log.error("Failed to publish trip.event_recorded for trip {}: {}", trip.getId(), ex.getMessage());
        }

        TripResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_TRIP_EVENT_RECORDED_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripEventDto(TripMapper.toEventDto(event));
        return response;
    }

    /**
     * Record a GPS location ping as a CHECKPOINT event.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripResponse recordLocation(RecordLocationRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = tripServiceValidator.isRecordLocationRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_TRIP_LOCATION_RECORD_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load trip
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(request.getTripId(), EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Record CHECKPOINT event
        // ============================================================
        TripEvent event = recordTripEvent(trip, TripEventType.CHECKPOINT, request.getLatitude(), request.getLongitude(),
                null, null, username, LocalDateTime.now(), locale);

        TripResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_TRIP_LOCATION_RECORDED_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripEventDto(TripMapper.toEventDto(event));
        return response;
    }

    /**
     * Trigger arrival: sets trip ARRIVED → OTP_PENDING, generates OTP, sends notification.
     *
     * Flow:
     * 1. Validate
     * 2. Load trip and assert IN_TRANSIT
     * 3. Set ARRIVED and OTP_PENDING
     * 4. Generate 6-digit OTP, BCrypt hash, persist delivery_otp (expires 30 min)
     * 5. Publish OTP notification via RabbitMQ notifications.direct
     * 6. Record ARRIVED + OTP_SENT events
     * 7. Update shipment to ARRIVED_PENDING_OTP
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripResponse triggerArrival(TriggerArrivalRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = tripServiceValidator.isTriggerArrivalRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_TRIP_ARRIVAL_TRIGGER_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load and lock trip
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(request.getTripId(), EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.IN_TRANSIT) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_IN_TRANSIT.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Transition trip status → OTP_PENDING
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        trip.setStatus(TripStatus.OTP_PENDING);
        trip.setArrivedAt(now);
        trip.setModifiedAt(now);
        trip.setModifiedBy(username);
        tripServiceAuditable.update(trip, locale, username);

        // ============================================================
        // STEP 4: Generate and store OTP
        // ============================================================
        String rawOtp = generateSixDigitOtp();
        String otpHash = bCryptPasswordEncoder.encode(rawOtp);
        LocalDateTime otpExpiry = now.plusMinutes(30);

        DeliveryOtp deliveryOtp = new DeliveryOtp();
        deliveryOtp.setTrip(trip);
        deliveryOtp.setOtpCodeHash(otpHash);
        deliveryOtp.setExpiresAt(otpExpiry);
        deliveryOtp.setSentToUserId(request.getDriverUserId());
        deliveryOtp.setSentAt(now);
        deliveryOtp.setEntityStatus(EntityStatus.ACTIVE);
        deliveryOtp.setCreatedAt(now);
        deliveryOtp.setCreatedBy(username);
        deliveryOtpServiceAuditable.create(deliveryOtp, locale, username);
        log.info("OTP generated for trip {} expires at {}", trip.getId(), otpExpiry);

        // ============================================================
        // STEP 5: Publish OTP notification via notifications.direct
        // ============================================================
        try {
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("templateCode", "DELIVERY_ARRIVAL_OTP");
            notificationPayload.put("tripId", trip.getId());
            notificationPayload.put("tripNumber", trip.getTripNumber());
            notificationPayload.put("recipientUserId", request.getDriverUserId());
            notificationPayload.put("otp", rawOtp); // dev-mode: include raw OTP for SMS
            notificationPayload.put("expiresAt", otpExpiry.toString());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notificationPayload);
            log.info("OTP notification enqueued for trip {}", trip.getId());
        } catch (Exception ex) {
            log.error("Failed to publish OTP notification for trip {}: {}", trip.getId(), ex.getMessage());
        }

        // ============================================================
        // STEP 6: Record ARRIVED + OTP_SENT events
        // ============================================================
        recordTripEvent(trip, TripEventType.ARRIVED, null, null,
                "Driver arrived at destination", request.getDriverUserId(), username, now, locale);
        recordTripEvent(trip, TripEventType.OTP_SENT, null, null,
                "OTP sent to user " + request.getDriverUserId(), request.getDriverUserId(), username, now, locale);

        // ============================================================
        // STEP 7: Update shipment → ARRIVED_PENDING_OTP
        // ============================================================
        try {
            UpdateShipmentStatusFeignRequest statusUpdate = new UpdateShipmentStatusFeignRequest();
            statusUpdate.setShipmentId(trip.getShipmentId());
            statusUpdate.setStatus("ARRIVED_PENDING_OTP");
            statusUpdate.setTripId(trip.getId());
            shipmentManagementServiceClient.updateShipmentStatus(statusUpdate, locale);
        } catch (Exception ex) {
            log.error("Failed to update shipment {} to ARRIVED_PENDING_OTP: {}", trip.getShipmentId(), ex.getMessage());
        }

        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_ARRIVAL_TRIGGERED_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDto(TripMapper.toDto(trip));
        return response;
    }

    /**
     * Verify OTP and complete delivery.
     *
     * Flow:
     * 1. Validate request
     * 2. Load trip, assert OTP_PENDING
     * 3. Find active OTP; verify hash and expiry
     * 4. Call inventory complete-with-grv (idempotency key = tripNumber + "-GRV")
     * 5. Set trip DELIVERED, shipment DELIVERED
     * 6. Record OTP_VERIFIED + DELIVERED events
     * 7. Publish trip.delivered
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripResponse verifyDeliveryOtp(VerifyDeliveryOtpRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = tripServiceValidator.isVerifyDeliveryOtpRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_TRIP_DELIVERY_VERIFY_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load trip and assert OTP_PENDING
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(request.getTripId(), EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.OTP_PENDING) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_OTP_PENDING.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Find and verify OTP
        // ============================================================
        DeliveryOtp deliveryOtp = deliveryOtpRepository
                .findTopByTripIdAndVerifiedAtIsNullAndEntityStatusNotOrderByIdDesc(trip.getId(), EntityStatus.DELETED)
                .orElse(null);
        if (deliveryOtp == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_OTP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (deliveryOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_OTP_INVALID_OR_EXPIRED.getCode(), new String[]{}, locale));
        }
        if (!bCryptPasswordEncoder.matches(request.getOtp(), deliveryOtp.getOtpCodeHash())) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_OTP_INVALID_OR_EXPIRED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 4: Mark OTP verified
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        deliveryOtp.setVerifiedAt(now);
        deliveryOtp.setModifiedAt(now);
        deliveryOtp.setModifiedBy(username);
        deliveryOtpServiceAuditable.update(deliveryOtp, locale, username);

        // ============================================================
        // STEP 5: Call inventory complete-with-grv
        // ============================================================
        if (trip.getInventoryTransferId() != null) {
            try {
                String idempotencyKey = trip.getTripNumber() + "-GRV";
                InventoryCompleteWithGrvDto completeDto = new InventoryCompleteWithGrvDto();
                completeDto.setTransferId(trip.getInventoryTransferId());
                completeDto.setReceivedByUserId(request.getReceiverUserId());
                completeDto.setIdempotencyKey(idempotencyKey);
                inventoryManagementServiceClient.completeWithGrv(completeDto, locale);
                log.info("Inventory transfer {} completed with GRV for trip {}", trip.getInventoryTransferId(), trip.getId());
            } catch (Exception ex) {
                log.error("Failed to complete-with-grv for transfer {}: {}", trip.getInventoryTransferId(), ex.getMessage());
            }
        }

        // ============================================================
        // STEP 6: Set trip DELIVERED
        // ============================================================
        trip.setStatus(TripStatus.DELIVERED);
        trip.setCompletedAt(now);
        trip.setReceiverUserId(request.getReceiverUserId());
        trip.setModifiedAt(now);
        trip.setModifiedBy(username);
        tripServiceAuditable.update(trip, locale, username);

        // ============================================================
        // STEP 7: Update shipment → DELIVERED
        // ============================================================
        try {
            UpdateShipmentStatusFeignRequest statusUpdate = new UpdateShipmentStatusFeignRequest();
            statusUpdate.setShipmentId(trip.getShipmentId());
            statusUpdate.setStatus("DELIVERED");
            statusUpdate.setTripId(trip.getId());
            shipmentManagementServiceClient.updateShipmentStatus(statusUpdate, locale);
        } catch (Exception ex) {
            log.error("Failed to update shipment {} to DELIVERED: {}", trip.getShipmentId(), ex.getMessage());
        }

        // ============================================================
        // STEP 8: Record OTP_VERIFIED + DELIVERED events
        // ============================================================
        recordTripEvent(trip, TripEventType.OTP_VERIFIED, null, null,
                "OTP verified by user " + request.getReceiverUserId(), request.getReceiverUserId(), username, now, locale);
        recordTripEvent(trip, TripEventType.DELIVERED, null, null,
                "Delivery confirmed", request.getReceiverUserId(), username, now, locale);

        // ============================================================
        // STEP 9: Publish trip.delivered
        // ============================================================
        publishTripEvent(RabbitMQProducerConfig.ROUTING_KEY_TRIP_DELIVERED, trip);

        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_DELIVERY_VERIFIED_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDto(TripMapper.toDto(trip));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse findById(Long id, Locale locale, String username) {
        Trip trip = tripRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        List<TripEvent> events = tripEventRepository
                .findTop10ByTripIdAndEntityStatusNotOrderByEventTimeDesc(trip.getId(), EntityStatus.DELETED);
        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_FIND_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDto(TripMapper.toDtoWithEvents(trip, events));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse findByMultipleFilters(TripFilterRequest request, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        TripStatus statusFilter = null;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                statusFilter = TripStatus.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown trip status filter: {}", request.getStatus());
            }
        }

        int page = Math.max(0, request.getPage());
        int size = (request.getSize() > 0 && request.getSize() <= 100) ? request.getSize() : 20;

        Page<Trip> trips = tripRepository.findByFilters(
                organizationId, statusFilter,
                request.getSearchTerm(),
                EntityStatus.DELETED,
                PageRequest.of(page, size));

        List<TripDto> dtos = trips.getContent().stream().map(TripMapper::toDto).toList();

        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_FIND_ALL_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDtoList(dtos);
        return response;
    }

    /**
     * Public tracking view — no authentication required.
     * Returns full event timeline for the trip.
     */
    @Override
    @Transactional(readOnly = true)
    public TripResponse track(Long id, Locale locale) {
        Trip trip = tripRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        List<TripEvent> events = tripEventRepository
                .findByTripIdAndEntityStatusNotOrderByEventTimeDesc(trip.getId(), EntityStatus.DELETED);
        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_TRACK_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDto(TripMapper.toDtoWithEvents(trip, events));
        return response;
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private TripEvent recordTripEvent(Trip trip, TripEventType type,
                                      BigDecimal lat, BigDecimal lng,
                                      String notes, Long recordedByUserId,
                                      String createdBy, LocalDateTime eventTime, Locale locale) {
        TripEvent event = new TripEvent();
        event.setTrip(trip);
        event.setEventType(type);
        event.setEventTime(eventTime);
        event.setLatitude(lat);
        event.setLongitude(lng);
        event.setNotes(notes);
        event.setRecordedByUserId(recordedByUserId);
        event.setEntityStatus(EntityStatus.ACTIVE);
        event.setCreatedAt(eventTime);
        event.setCreatedBy(createdBy);
        return tripEventServiceAuditable.create(event, locale, createdBy);
    }

    private void publishTripEvent(String routingKey, Trip trip) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tripId", trip.getId());
            payload.put("tripNumber", trip.getTripNumber());
            payload.put("organizationId", trip.getOrganizationId());
            payload.put("shipmentId", trip.getShipmentId());
            payload.put("status", trip.getStatus().name());
            rabbitTemplate.convertAndSend(RabbitMQProducerConfig.TRIP_EXCHANGE, routingKey, payload);
            log.info("Published {} for trip {}", routingKey, trip.getId());
        } catch (Exception ex) {
            log.error("Failed to publish {} for trip {}: {}", routingKey, trip.getId(), ex.getMessage());
        }
    }

    private String generateSixDigitOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private TripResponse successResponse(int statusCode, String message) {
        TripResponse response = new TripResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private TripResponse errorResponse(int statusCode, String message) {
        return errorResponse(statusCode, message, new ArrayList<>());
    }

    private TripResponse errorResponse(int statusCode, String message, List<String> errors) {
        TripResponse response = new TripResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
