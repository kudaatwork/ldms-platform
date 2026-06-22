package projectlx.trip.tracking.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.trip.tracking.business.auditable.api.DeliveryOtpServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripEventServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripServiceAuditable;
import projectlx.trip.tracking.business.logic.api.TripService;
import projectlx.co.zw.shared_library.billing.PlatformWalletActionCodes;
import projectlx.co.zw.shared_library.billing.PlatformWalletUsageSupport;
import projectlx.trip.tracking.business.logic.support.TripDeliveryWorkflowBootstrapSupport;
import projectlx.trip.tracking.business.logic.support.CallerOrganizationResolver;
import projectlx.trip.tracking.business.logic.support.TripDriverPortalSupport;
import projectlx.trip.tracking.business.logic.support.LogisticsNotificationRecipientResolver;
import projectlx.trip.tracking.business.logic.support.TripIotDemoSimulator;
import projectlx.trip.tracking.business.logic.support.TripMapper;
import projectlx.trip.tracking.business.logic.support.TripNumberGenerator;
import projectlx.trip.tracking.business.logic.support.ShipmentTripStartLock;
import projectlx.trip.tracking.business.logic.support.TripRoutePlannerSupport;
import projectlx.trip.tracking.business.logic.support.TripTelemetryPublisher;
import projectlx.trip.tracking.model.TripRoutePlan;
import projectlx.trip.tracking.repository.TripRoutePlanRepository;
import projectlx.trip.tracking.utils.config.IotIntegrationProperties;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;
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
import projectlx.trip.tracking.utils.dtos.InventoryCompleteSalesOrderWithGrvDto;
import projectlx.trip.tracking.utils.dtos.InventoryCompleteWithGrvDto;
import projectlx.trip.tracking.utils.dtos.InventoryStartSalesOrderDispatchDto;
import projectlx.trip.tracking.utils.dtos.InventoryStartTransitDto;
import projectlx.trip.tracking.utils.dtos.ShipmentSummaryDto;
import projectlx.trip.tracking.utils.requests.UpdateShipmentStatusFeignRequest;
import projectlx.trip.tracking.utils.responses.ShipmentFeignResponse;
import projectlx.trip.tracking.utils.dtos.DriverTripMetricsDto;
import projectlx.trip.tracking.utils.dtos.DriverTripSummaryDto;
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
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.notifications.LogisticsLifecycleNotificationSupport;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class TripServiceImpl implements TripService {

  private static final List<TripStatus> ACTIVE_TRIP_STATUSES = List.of(
            TripStatus.SCHEDULED,
            TripStatus.IN_TRANSIT,
            TripStatus.AT_BORDER_HOLD,
            TripStatus.ROADSIDE_HOLD,
            TripStatus.ARRIVED,
            TripStatus.COUNTING_STOCK,
            TripStatus.COUNT_COMPLETE,
            TripStatus.OTP_PENDING,
            TripStatus.RETURN_IN_TRANSIT);

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
    private final TripRoutePlannerSupport routePlannerSupport;
    private final TripIotDemoSimulator demoSimulator;
    private final TripTelemetryPublisher telemetryPublisher;
    private final TripRoutePlanRepository tripRoutePlanRepository;
    private final IotIntegrationProperties iotProperties;
    private final ShipmentTripStartLock shipmentTripStartLock;
    private final LogisticsLifecycleNotificationSupport logisticsLifecycleNotificationSupport;
    private final LogisticsNotificationRecipientResolver recipientResolver;
    private final TripDriverPortalSupport tripDriverPortalSupport;
    private final TripDeliveryWorkflowBootstrapSupport workflowBootstrap;
    private final PlatformWalletUsageSupport platformWalletUsageSupport;

    private static final List<TripStatus> ARRIVAL_OR_DELIVERY_STATUSES = List.of(
            TripStatus.ARRIVED,
            TripStatus.COUNTING_STOCK,
            TripStatus.COUNT_COMPLETE,
            TripStatus.OTP_PENDING);

    private static final List<TripStatus> ACTIVE_STATUSES = List.of(
            TripStatus.SCHEDULED, TripStatus.IN_TRANSIT, TripStatus.AT_BORDER_HOLD,
            TripStatus.ROADSIDE_HOLD, TripStatus.ARRIVED, TripStatus.COUNTING_STOCK,
            TripStatus.COUNT_COMPLETE, TripStatus.OTP_PENDING);

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
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TripResponse startTrip(StartTripRequest request, Locale locale, String username) {

        if (!shipmentTripStartLock.tryLock(request.getShipmentId())) {
            return errorResponse(409, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_START_LOCK_UNAVAILABLE.getCode(), new String[]{}, locale));
        }
        try {
            return doStartTrip(request, locale, username);
        } finally {
            shipmentTripStartLock.unlock(request.getShipmentId());
        }
    }

    private TripResponse doStartTrip(StartTripRequest request, Locale locale, String username) {

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
        ShipmentSummaryDto shipment = fetchAllocatedShipmentForTripStart(
                request.getShipmentId(),
                request.getInventoryTransferId(),
                request.getSalesOrderId(),
                locale);
        if (shipment == null) {
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
        trip.setShipmentNumber(shipment.getShipmentNumber());
        trip.setInventoryTransferId(shipment.getInventoryTransferId());
        trip.setSalesOrderId(shipment.getSalesOrderId());
        trip.setFleetDriverId(request.getFleetDriverId());
        trip.setFleetAssetId(request.getFleetAssetId());
        trip.setStatus(TripStatus.IN_TRANSIT);
        trip.setStartedAt(now);
        trip.setFromWarehouseName(shipment.getFromWarehouseName());
        trip.setToWarehouseName(shipment.getToWarehouseName());
        trip.setProductName(shipment.getProductName());
        trip.setProductCode(shipment.getProductCode());
        trip.setQuantity(shipment.getQuantity());
        trip.setEntityStatus(EntityStatus.ACTIVE);
        trip.setCreatedAt(now);
        trip.setCreatedBy(username);

        Trip saved = tripServiceAuditable.create(trip, locale, username);
        log.info("Trip created: id={} number={} shipment={} org={}",
                saved.getId(), saved.getTripNumber(), saved.getShipmentId(), organizationId);

        platformWalletUsageSupport.chargeRequired(
                organizationId,
                PlatformWalletActionCodes.TRIP_CREATE,
                "TRIP",
                saved.getId(),
                saved.getId());

        // ============================================================
        // STEP 6: Start inventory movement for linked source document
        // ============================================================
        if (saved.getInventoryTransferId() != null) {
            try {
                InventoryStartTransitDto startTransitDto = new InventoryStartTransitDto();
                startTransitDto.setTransferId(saved.getInventoryTransferId());
                startTransitDto.setStartedByUserId(request.getStartedByUserId());
                startTransitDto.setTripId(saved.getId());
                startTransitDto.setShipmentId(saved.getShipmentId());
                inventoryManagementServiceClient.startTransit(startTransitDto, locale);
                log.info("Inventory transfer {} started for trip {}", saved.getInventoryTransferId(), saved.getId());
            } catch (Exception ex) {
                log.error("Failed to start inventory transit for transfer {}: {}", saved.getInventoryTransferId(), ex.getMessage());
            }
        } else if (saved.getSalesOrderId() != null
                || "SALES_ORDER".equalsIgnoreCase(shipment.getSourceType())) {
            Long salesOrderId = saved.getSalesOrderId() != null ? saved.getSalesOrderId() : shipment.getSalesOrderId();
            try {
                InventoryStartSalesOrderDispatchDto dispatchDto = new InventoryStartSalesOrderDispatchDto();
                dispatchDto.setSalesOrderId(salesOrderId);
                dispatchDto.setStartedByUserId(request.getStartedByUserId());
                dispatchDto.setTripId(saved.getId());
                dispatchDto.setShipmentId(saved.getShipmentId());
                inventoryManagementServiceClient.startSalesOrderDispatch(dispatchDto, locale);
                log.info("Sales order {} dispatch started for trip {}", salesOrderId, saved.getId());
            } catch (Exception ex) {
                log.error("Failed to start sales order dispatch for SO {}: {}", salesOrderId, ex.getMessage());
            }
        }

        // ============================================================
        // STEP 7: Update shipment status → IN_TRANSIT (required)
        // ============================================================
        try {
            UpdateShipmentStatusFeignRequest statusUpdate = new UpdateShipmentStatusFeignRequest();
            statusUpdate.setShipmentId(saved.getShipmentId());
            statusUpdate.setStatus("IN_TRANSIT");
            statusUpdate.setTripId(saved.getId());
            shipmentManagementServiceClient.updateShipmentStatus(statusUpdate, locale);
        } catch (Exception ex) {
            log.error("Failed to update shipment {} to IN_TRANSIT: {}", saved.getShipmentId(), ex.getMessage());
            throw new IllegalStateException(messageService.getMessage(
                    I18Code.MESSAGE_TRIP_SHIPMENT_STATUS_SYNC_FAILED.getCode(), new String[]{}, locale), ex);
        }

        cancelSupersededActiveTrips(saved.getShipmentId(), saved.getId(), locale, username);

        // ============================================================
        // STEP 8: Record DEPARTED event
        // ============================================================
        recordTripEvent(saved, TripEventType.DEPARTED, null, null,
                "Trip started by " + username, request.getStartedByUserId(), username, now, locale);

        // ============================================================
        // STEP 9: Publish trip.started event
        // ============================================================
        publishTripEvent(RabbitMQProducerConfig.ROUTING_KEY_TRIP_STARTED, saved);

        // ============================================================
        // STEP 10: Send trip-started lifecycle notifications (non-blocking)
        // ============================================================
        sendTripStartedNotification(saved, locale, username);

        routePlannerSupport.ensureRoutePlan(saved, username);
        if (iotProperties.isAutoStartDemoSimulation()) {
            demoSimulator.startSimulation(saved.getId(), username);
        }

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
        LocalDateTime now = LocalDateTime.now();
        TripEvent event = recordTripEvent(trip, eventType, request.getLatitude(), request.getLongitude(),
                request.getNotes(), null, username, now, locale);

        // ============================================================
        // STEP 4: Apply status transition driven by event type
        // ============================================================
        TripStatus previousStatus = trip.getStatus();
        TripStatus newStatus = resolveStatusTransition(previousStatus, eventType);
        if (newStatus != null && newStatus != previousStatus) {
            trip.setStatus(newStatus);
            trip.setModifiedAt(now);
            trip.setModifiedBy(username);
            tripServiceAuditable.update(trip, locale, username);
            log.info("Trip {} status transitioned {} → {} on event {}",
                    trip.getId(), previousStatus, newStatus, eventType);
        }

        // ============================================================
        // STEP 5: Publish trip.event_recorded
        // ============================================================
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tripId", trip.getId());
            payload.put("tripNumber", trip.getTripNumber());
            payload.put("eventType", eventType.name());
            payload.put("status", trip.getStatus().name());
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
     * Records a system-initiated trip event (e.g. from fuel-expenses on fund request approval).
     * Delegates to {@link #recordEvent} with username="system".
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripResponse recordSystemEvent(RecordTripEventRequest request, Locale locale) {
        return recordEvent(request, locale, "system");
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

        platformWalletUsageSupport.chargeRequired(
                trip.getOrganizationId(),
                PlatformWalletActionCodes.TRIP_TRACK,
                "TRIP",
                trip.getId(),
                trip.getId());

        // ============================================================
        // STEP 3: Record CHECKPOINT event
        // ============================================================
        TripEvent event = recordTripEvent(trip, TripEventType.CHECKPOINT, request.getLatitude(), request.getLongitude(),
                null, null, username, LocalDateTime.now(), locale);

        tripRoutePlanRepository.findByTripIdAndEntityStatusNot(trip.getId(), EntityStatus.DELETED).ifPresent(plan -> {
            plan.setCurrentLatitude(request.getLatitude());
            plan.setCurrentLongitude(request.getLongitude());
            plan.setModifiedAt(LocalDateTime.now());
            plan.setModifiedBy(username);
            tripRoutePlanRepository.save(plan);
            publishLiveTelemetry(trip, plan);
        });

        TripResponse response = successResponse(201,
                messageService.getMessage(I18Code.MESSAGE_TRIP_LOCATION_RECORDED_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripEventDto(TripMapper.toEventDto(event));
        return response;
    }

    private void publishLiveTelemetry(Trip trip, TripRoutePlan plan) {
        TripLiveSnapshotDto snapshot = telemetryPublisher.buildSnapshot(
                trip,
                plan.getCurrentLatitude(),
                plan.getCurrentLongitude(),
                plan.getCurrentSpeedKmh(),
                plan.getCurrentHeadingDeg(),
                plan.getOverallProgressPct(),
                plan.isSimulationActive(),
                plan.isSimulationActive());
        snapshot.setRouteWaypoints(routePlannerSupport.buildFullPath(plan));
        telemetryPublisher.publish(trip, snapshot);
    }

    /**
     * Trigger arrival: sets trip status ARRIVED, creates the delivery workflow, notifies shipment service.
     *
     * NOTE: OTP generation has been moved to TripDeliveryService.sendDeliveryOtp — call that endpoint
     * after stock counting is complete (COUNT_COMPLETE) to generate and send the OTP.
     *
     * Flow:
     * 1. Validate
     * 2. Load trip and assert IN_TRANSIT
     * 3. Set status ARRIVED, record arrivedAt
     * 4. Create/ensure trip_delivery_workflow
     * 5. Record ARRIVED event
     * 6. Update shipment to ARRIVED_PENDING_OTP
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
        // STEP 2: Load trip — IN_TRANSIT required unless already in delivery phase (idempotent)
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(request.getTripId(), EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.IN_TRANSIT) {
            if (ARRIVAL_OR_DELIVERY_STATUSES.contains(trip.getStatus())) {
                LocalDateTime now = LocalDateTime.now();
                workflowBootstrap.ensureWorkflow(trip, now, username, locale);
                TripResponse idempotent = successResponse(200, messageService.getMessage(
                        I18Code.MESSAGE_TRIP_ARRIVAL_TRIGGERED_SUCCESS.getCode(), new String[]{}, locale));
                idempotent.setTripDto(TripMapper.toDto(trip));
                return idempotent;
            }
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_IN_TRANSIT.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Transition trip status → ARRIVED
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        trip.setStatus(TripStatus.ARRIVED);
        trip.setArrivedAt(now);
        trip.setModifiedAt(now);
        trip.setModifiedBy(username);
        tripServiceAuditable.update(trip, locale, username);
        log.info("Trip {} status set to ARRIVED at {}", trip.getId(), now);

        workflowBootstrap.ensureWorkflow(trip, now, username, locale);

        // ============================================================
        // STEP 4: Record ARRIVED event
        // ============================================================
        recordTripEvent(trip, TripEventType.ARRIVED, null, null,
                "Driver arrived at destination", request.getDriverUserId(), username, now, locale);

        // ============================================================
        // STEP 5: Update shipment → ARRIVED_PENDING_OTP
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
        } else if (trip.getSalesOrderId() != null) {
            try {
                String idempotencyKey = trip.getTripNumber() + "-GRV";
                InventoryCompleteSalesOrderWithGrvDto completeDto = new InventoryCompleteSalesOrderWithGrvDto();
                completeDto.setSalesOrderId(trip.getSalesOrderId());
                completeDto.setReceivedByUserId(request.getReceiverUserId());
                completeDto.setIdempotencyKey(idempotencyKey);
                inventoryManagementServiceClient.completeSalesOrderWithGrv(completeDto, locale);
                log.info("Sales order {} completed with customer GRV for trip {}", trip.getSalesOrderId(), trip.getId());
            } catch (Exception ex) {
                log.error("Failed to complete-with-grv for sales order {}: {}", trip.getSalesOrderId(), ex.getMessage());
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

        platformWalletUsageSupport.chargeRequired(
                trip.getOrganizationId(),
                PlatformWalletActionCodes.TRIP_COMPLETE,
                "TRIP",
                trip.getId(),
                trip.getId());

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

        // ============================================================
        // STEP 10: Send trip-completed lifecycle notifications (non-blocking)
        // ============================================================
        sendTripCompletedNotification(trip, locale, username);

        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_DELIVERY_VERIFIED_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDto(TripMapper.toDto(trip));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse findById(Long id, Locale locale, String username) {
        Trip trip = tripRepository.findByIdAndEntityStatusNotNoLock(id, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        List<TripEvent> events = loadTimelineEvents(trip.getId(), PageRequest.of(0, 10));
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

        TripFilterRequest filters = request != null ? request : new TripFilterRequest();

        TripStatus statusFilter = resolveStatusFilter(filters.getStatus());

        int page = Math.max(0, filters.getPage());
        int size = (filters.getSize() > 0 && filters.getSize() <= 100) ? filters.getSize() : 20;

        Page<Trip> trips;
        if (Boolean.TRUE.equals(filters.getActiveOnly())) {
            trips = tripRepository.findActiveByFilters(
                    organizationId,
                    ACTIVE_TRIP_STATUSES,
                    filters.getSearchTerm(),
                    EntityStatus.DELETED,
                    PageRequest.of(page, size));
        } else {
            trips = tripRepository.findByFilters(
                    organizationId, statusFilter,
                    filters.getSearchTerm(),
                    EntityStatus.DELETED,
                    PageRequest.of(page, size));
        }

        List<TripDto> dtos = dedupeActiveTripsPerShipment(
                trips.getContent().stream().map(TripMapper::toDto).toList());

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
        Trip trip = tripRepository.findByIdAndEntityStatusNotNoLock(id, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        List<TripEvent> events = loadTimelineEvents(trip.getId(), PageRequest.of(0, 100));
        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_TRACK_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDto(TripMapper.toDtoWithEvents(trip, events));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse listMyTrips(Locale locale, String username) {
        Long fleetDriverId = tripDriverPortalSupport.resolveFleetDriverId(username, locale);
        if (fleetDriverId == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_PROFILE_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        List<DriverTripSummaryDto> trips = tripDriverPortalSupport.listTripsForDriver(fleetDriverId, locale);
        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_FIND_ALL_SUCCESS.getCode(), new String[]{}, locale));
        response.setDriverTripSummaryDtoList(trips);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse findMyTripById(Long tripId, Locale locale, String username) {
        Long sessionUserId = tripDriverPortalSupport.resolveSessionUserId(username, locale);
        Long fleetDriverId = tripDriverPortalSupport.resolveFleetDriverId(username, locale);
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);

        Trip trip = tripRepository.findByIdAndEntityStatusNotNoLock(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        boolean driverAccess = fleetDriverId != null
                && tripDriverPortalSupport.isTripAccessibleToDriver(trip, fleetDriverId, sessionUserId, locale);
        boolean organizationAccess = organizationId != null && organizationId.equals(trip.getOrganizationId());
        if (!driverAccess && !organizationAccess) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_FIND_SUCCESS.getCode(), new String[]{}, locale));
        response.setDriverTripSummaryDto(tripDriverPortalSupport.toSummary(trip, locale));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getMyTripMetrics(Locale locale, String username) {
        Long fleetDriverId = tripDriverPortalSupport.resolveFleetDriverId(username, locale);
        if (fleetDriverId == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_DRIVER_PROFILE_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        DriverTripMetricsDto metrics = tripDriverPortalSupport.metricsForDriver(fleetDriverId);
        TripResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_FIND_ALL_SUCCESS.getCode(), new String[]{}, locale));
        response.setDriverTripMetricsDto(metrics);
        return response;
    }

    /**
     * Milestone events for operator timelines — excludes high-frequency GPS CHECKPOINT pings.
     */
    private List<TripEvent> loadTimelineEvents(Long tripId, Pageable pageable) {
        return tripEventRepository.findByTrip_IdAndEventTypeNotAndEntityStatusNotOrderByEventTimeDesc(
                tripId, TripEventType.CHECKPOINT, EntityStatus.DELETED, pageable);
    }

    // ============================================================
    // Notification helpers
    // ============================================================

    private void sendTripStartedNotification(Trip trip, Locale locale, String performedBy) {
        try {
            OrganizationDto org = recipientResolver.resolveOrganization(trip.getOrganizationId(), locale);
            List<UserDto> fleetManagers = recipientResolver.resolveFleetManagers(trip.getOrganizationId(), locale);
            LogisticsNotificationRecipientResolver.DriverContact driver =
                    recipientResolver.resolveDriverContact(trip.getFleetDriverId(), locale);

            String orgEmail = org != null ? org.getEmail() : null;
            String orgPhone = org != null ? org.getPhoneNumber() : null;
            String contactEmail = org != null ? org.getContactPersonEmail() : null;
            String contactPhone = org != null ? org.getContactPersonPhoneNumber() : null;
            String orgName = org != null ? org.getName() : "";
            String contactName = org != null
                    ? buildOrgContactName(org.getContactPersonFirstName(), org.getContactPersonLastName(), orgName)
                    : "";

            logisticsLifecycleNotificationSupport.notifyTripStarted(
                    trip.getOrganizationId(),
                    orgEmail, orgPhone, contactEmail, contactPhone, orgName, contactName,
                    fleetManagers,
                    driver.email(), driver.phone(), driver.name(),
                    trip.getTripNumber(),
                    null,
                    trip.getFromWarehouseName(),
                    trip.getToWarehouseName(),
                    trip.getProductName(),
                    performedBy);
        } catch (Exception ex) {
            log.error("Failed to send trip-started notification for tripId={}: {}", trip.getId(), ex.getMessage());
        }
    }

    private void sendTripCompletedNotification(Trip trip, Locale locale, String performedBy) {
        try {
            OrganizationDto org = recipientResolver.resolveOrganization(trip.getOrganizationId(), locale);
            List<UserDto> fleetManagers = recipientResolver.resolveFleetManagers(trip.getOrganizationId(), locale);
            LogisticsNotificationRecipientResolver.DriverContact driver =
                    recipientResolver.resolveDriverContact(trip.getFleetDriverId(), locale);

            String orgEmail = org != null ? org.getEmail() : null;
            String orgPhone = org != null ? org.getPhoneNumber() : null;
            String contactEmail = org != null ? org.getContactPersonEmail() : null;
            String contactPhone = org != null ? org.getContactPersonPhoneNumber() : null;
            String orgName = org != null ? org.getName() : "";
            String contactName = org != null
                    ? buildOrgContactName(org.getContactPersonFirstName(), org.getContactPersonLastName(), orgName)
                    : "";

            logisticsLifecycleNotificationSupport.notifyTripCompleted(
                    trip.getOrganizationId(),
                    orgEmail, orgPhone, contactEmail, contactPhone, orgName, contactName,
                    fleetManagers,
                    driver.email(), driver.phone(), driver.name(),
                    trip.getTripNumber(),
                    null,
                    trip.getFromWarehouseName(),
                    trip.getToWarehouseName(),
                    trip.getProductName(),
                    performedBy);
        } catch (Exception ex) {
            log.error("Failed to send trip-completed notification for tripId={}: {}", trip.getId(), ex.getMessage());
        }
    }

    private static String buildOrgContactName(String firstName, String lastName, String fallback) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? fallback : name;
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private ShipmentSummaryDto fetchAllocatedShipmentForTripStart(
            Long shipmentId, Long inventoryTransferId, Long salesOrderId, Locale locale) {
        if (shipmentId == null) {
            return null;
        }
        ShipmentSummaryDto dto = null;
        if (inventoryTransferId != null) {
            dto = fetchShipmentSummary(() ->
                    shipmentManagementServiceClient.findShipmentByTransferId(inventoryTransferId, locale),
                    "transfer", inventoryTransferId);
            if (dto != null && !shipmentId.equals(dto.getId())) {
                log.warn("Inventory transfer {} is linked to shipment {} but trip start referenced shipment {}",
                        inventoryTransferId, dto.getId(), shipmentId);
                dto = null;
            }
        }
        if (dto == null && salesOrderId != null) {
            dto = fetchShipmentSummary(() ->
                    shipmentManagementServiceClient.findShipmentBySalesOrderId(salesOrderId, locale),
                    "sales-order", salesOrderId);
            if (dto != null && !shipmentId.equals(dto.getId())) {
                log.warn("Sales order {} is linked to shipment {} but trip start referenced shipment {}",
                        salesOrderId, dto.getId(), shipmentId);
                dto = null;
            }
        }
        if (dto == null) {
            dto = fetchShipmentSummary(() ->
                    shipmentManagementServiceClient.findShipmentById(shipmentId, locale),
                    "shipment", shipmentId);
        }
        if (dto == null) {
            return null;
        }
        if (!"ALLOCATED".equalsIgnoreCase(dto.getStatus())) {
            log.warn("Shipment {} is not ALLOCATED for trip start (status={})", shipmentId, dto.getStatus());
            return null;
        }
        return dto;
    }

    private ShipmentSummaryDto fetchShipmentSummary(
            java.util.function.Supplier<ShipmentFeignResponse> loader,
            String lookupKind,
            Long lookupId) {
        try {
            ShipmentFeignResponse response = loader.get();
            ShipmentSummaryDto dto = extractShipmentSummary(response);
            if (dto == null) {
                log.warn("Shipment lookup by {} {} returned no payload (success={}, statusCode={})",
                        lookupKind,
                        lookupId,
                        response != null && response.isSuccess(),
                        response != null ? response.getStatusCode() : null);
            }
            return dto;
        } catch (Exception ex) {
            log.warn("Could not fetch shipment by {} {} from shipment-management: {}",
                    lookupKind, lookupId, ex.getMessage());
            return null;
        }
    }

    private ShipmentSummaryDto extractShipmentSummary(ShipmentFeignResponse response) {
        if (response == null || response.getShipmentDto() == null) {
            return null;
        }
        if (!response.isSuccess() && response.getStatusCode() >= 400) {
            return null;
        }
        return response.getShipmentDto();
    }

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

    private void cancelSupersededActiveTrips(Long shipmentId, Long keepTripId, Locale locale, String username) {
        List<Trip> activeTrips = tripRepository.findByShipmentIdAndStatusNotInAndEntityStatusNotOrderByIdDesc(
                shipmentId, List.of(TripStatus.DELIVERED, TripStatus.CANCELLED), EntityStatus.DELETED);
        LocalDateTime now = LocalDateTime.now();
        for (Trip other : activeTrips) {
            if (other.getId().equals(keepTripId)) {
                continue;
            }
            other.setStatus(TripStatus.CANCELLED);
            other.setModifiedAt(now);
            other.setModifiedBy(username);
            tripServiceAuditable.update(other, locale, username);
            log.warn("Cancelled superseded trip id={} for shipment {} (keeping trip {})",
                    other.getId(), shipmentId, keepTripId);
        }
    }

    /**
     * When duplicate active trips exist for one shipment, expose only the newest row in list views.
     */
    private List<TripDto> dedupeActiveTripsPerShipment(List<TripDto> trips) {
        Set<Long> seenActiveShipments = new HashSet<>();
        List<TripDto> result = new ArrayList<>();
        for (TripDto dto : trips) {
            boolean terminal = dto.getStatus() == TripStatus.DELIVERED
                    || dto.getStatus() == TripStatus.RETURNED
                    || dto.getStatus() == TripStatus.CANCELLED;
            if (dto.getShipmentId() == null || terminal) {
                result.add(dto);
                continue;
            }
            if (seenActiveShipments.add(dto.getShipmentId())) {
                result.add(dto);
            }
        }
        return result;
    }

    private void publishTripEvent(String routingKey, Trip trip) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tripId", trip.getId());
            payload.put("tripNumber", trip.getTripNumber());
            payload.put("organizationId", trip.getOrganizationId());
            payload.put("shipmentId", trip.getShipmentId());
            payload.put("fleetDriverId", trip.getFleetDriverId());
            payload.put("fleetAssetId", trip.getFleetAssetId());
            payload.put("inventoryTransferId", trip.getInventoryTransferId());
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

    /**
     * Derives the new trip status from the incoming event type.
     * Returns {@code null} when no automatic transition applies for the current status.
     *
     * Transition table:
     *   ARRIVED_AT_BORDER             → AT_BORDER_HOLD   (only when IN_TRANSIT)
     *   BORDER_CLEARED                → IN_TRANSIT       (only when AT_BORDER_HOLD)
     *   ROADSIDE_FUEL_STOP            → ROADSIDE_HOLD    (only when IN_TRANSIT)
     *   ROADSIDE_MECHANIC_STOP        → ROADSIDE_HOLD    (only when IN_TRANSIT)
     *   ROADSIDE_RESUMED              → IN_TRANSIT       (only when ROADSIDE_HOLD)
     */
    private TripStatus resolveStatusTransition(TripStatus current, TripEventType eventType) {
        return switch (eventType) {
            case ARRIVED_AT_BORDER -> current == TripStatus.IN_TRANSIT ? TripStatus.AT_BORDER_HOLD : null;
            case BORDER_CLEARED    -> current == TripStatus.AT_BORDER_HOLD ? TripStatus.IN_TRANSIT : null;
            case ROADSIDE_FUEL_STOP, ROADSIDE_MECHANIC_STOP ->
                    current == TripStatus.IN_TRANSIT ? TripStatus.ROADSIDE_HOLD : null;
            case ROADSIDE_RESUMED  -> current == TripStatus.ROADSIDE_HOLD ? TripStatus.IN_TRANSIT : null;
            default                -> null;
        };
    }

    /**
     * Accepts portal aliases (e.g. IN_PROGRESS, PENDING) as well as canonical enum names.
     */
    private TripStatus resolveStatusFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING" -> TripStatus.SCHEDULED;
            case "IN_PROGRESS" -> TripStatus.IN_TRANSIT;
            default -> {
                try {
                    yield TripStatus.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    log.warn("Unknown trip status filter: {}", raw);
                    yield null;
                }
            }
        };
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
