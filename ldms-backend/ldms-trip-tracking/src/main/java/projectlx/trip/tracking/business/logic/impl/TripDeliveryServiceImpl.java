package projectlx.trip.tracking.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.trip.tracking.business.auditable.api.DeliveryOtpServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripDeliveryWorkflowServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripEventServiceAuditable;
import projectlx.trip.tracking.business.auditable.api.TripServiceAuditable;
import projectlx.trip.tracking.business.logic.api.TripDeliveryService;
import projectlx.trip.tracking.business.logic.support.TripDeliveryWorkflowBootstrapSupport;
import projectlx.trip.tracking.business.logic.support.TripMapper;
import projectlx.trip.tracking.business.validator.api.TripDeliveryServiceValidator;
import projectlx.trip.tracking.clients.InventoryManagementServiceClient;
import projectlx.trip.tracking.clients.ShipmentManagementServiceClient;
import projectlx.trip.tracking.model.DeliveryOtp;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripDeliveryReturnLine;
import projectlx.trip.tracking.model.TripDeliveryWorkflow;
import projectlx.trip.tracking.model.TripEvent;
import projectlx.trip.tracking.repository.DeliveryOtpRepository;
import projectlx.trip.tracking.repository.TripDeliveryWorkflowRepository;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.utils.config.RabbitMQProducerConfig;
import projectlx.trip.tracking.utils.dtos.InventoryCompleteSalesOrderWithGrvDto;
import projectlx.trip.tracking.utils.dtos.InventoryCompleteWithGrvDto;
import projectlx.trip.tracking.utils.dtos.TripDeliveryWorkflowDto;
import projectlx.trip.tracking.utils.enums.I18Code;
import projectlx.trip.tracking.utils.enums.TripEventType;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.trip.tracking.utils.requests.FinishCountingRequest;
import projectlx.trip.tracking.utils.requests.RecordReturnLinesRequest;
import projectlx.trip.tracking.utils.requests.ReturnLineItem;
import projectlx.trip.tracking.utils.requests.SendDeliveryOtpRequest;
import projectlx.trip.tracking.utils.requests.StartCountingRequest;
import projectlx.trip.tracking.utils.requests.UpdateShipmentStatusFeignRequest;
import projectlx.trip.tracking.utils.requests.VerifyDeliveryOtpRequest;
import projectlx.trip.tracking.utils.responses.TripDeliveryWorkflowResponse;
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
public class TripDeliveryServiceImpl implements TripDeliveryService {

    private final TripDeliveryServiceValidator validator;
    private final TripDeliveryWorkflowBootstrapSupport workflowBootstrap;
    private final TripDeliveryWorkflowServiceAuditable workflowAuditable;
    private final TripServiceAuditable tripServiceAuditable;
    private final TripEventServiceAuditable tripEventServiceAuditable;
    private final DeliveryOtpServiceAuditable deliveryOtpServiceAuditable;
    private final TripRepository tripRepository;
    private final TripDeliveryWorkflowRepository workflowRepository;
    private final DeliveryOtpRepository deliveryOtpRepository;
    private final ShipmentManagementServiceClient shipmentManagementServiceClient;
    private final InventoryManagementServiceClient inventoryManagementServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final MessageService messageService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    // ============================================================
    // GET WORKFLOW
    // ============================================================

    @Override
    @Transactional
    public TripDeliveryWorkflowResponse getWorkflow(Long tripId, Locale locale, String username) {
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        TripDeliveryWorkflow workflow = workflowRepository
                .findWithDetailsByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED)
                .orElse(null);

        if (workflow == null && workflowBootstrap.isDeliveryPhase(trip.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            workflowBootstrap.ensureWorkflow(trip, now, username, locale);
            workflow = workflowRepository
                    .findWithDetailsByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED)
                    .orElse(null);
        }

        TripDeliveryWorkflowResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_DELIVERY_WORKFLOW_FIND_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDto(TripMapper.toDto(trip));
        if (workflow != null) {
            response.setWorkflowDto(TripMapper.toWorkflowDto(workflow, trip.getId()));
        }
        return response;
    }

    // ============================================================
    // START COUNTING
    // ============================================================

    /**
     * Start stock counting for a given actor.
     *
     * Flow:
     * 1. Validate request
     * 2. Load trip — must be ARRIVED or COUNTING_STOCK
     * 3. Load or create the delivery workflow
     * 4. Record counting start timestamp for the actor
     * 5. Transition trip → COUNTING_STOCK (if first actor)
     * 6. Record STOCK_COUNTING_STARTED event
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripDeliveryWorkflowResponse startCounting(Long tripId, StartCountingRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = validator.isStartCountingRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DELIVERY_START_COUNTING_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load trip
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.ARRIVED && trip.getStatus() != TripStatus.COUNTING_STOCK) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_ARRIVED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Load or create workflow
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        TripDeliveryWorkflow workflow = workflowBootstrap.ensureWorkflow(trip, now, username, locale);

        // ============================================================
        // STEP 4: Record actor counting start timestamp
        // ============================================================
        String role = request.getActorRole().trim().toUpperCase();
        boolean alreadyStarted = false;
        switch (role) {
            case "DRIVER" -> {
                if (workflow.getDriverCountingStartedAt() != null) {
                    alreadyStarted = true;
                } else {
                    workflow.setDriverCountingStartedAt(now);
                }
            }
            case "CUSTOMER", "RECEIVER" -> {
                if (workflow.getCustomerCountingStartedAt() != null) {
                    alreadyStarted = true;
                } else {
                    workflow.setCustomerCountingStartedAt(now);
                }
            }
            default -> {
                return errorResponse(400, "Unknown actor role: " + role);
            }
        }

        if (alreadyStarted) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_DELIVERY_ALREADY_COUNTING.getCode(), new String[]{}, locale));
        }

        workflow.setModifiedAt(now);
        workflow.setModifiedBy(username);
        workflowAuditable.update(workflow, locale, username);

        // ============================================================
        // STEP 5: Transition trip → COUNTING_STOCK (first actor)
        // ============================================================
        if (trip.getStatus() == TripStatus.ARRIVED) {
            trip.setStatus(TripStatus.COUNTING_STOCK);
            trip.setModifiedAt(now);
            trip.setModifiedBy(username);
            tripServiceAuditable.update(trip, locale, username);
        }

        // ============================================================
        // STEP 6: Record STOCK_COUNTING_STARTED event
        // ============================================================
        recordTripEvent(trip, TripEventType.STOCK_COUNTING_STARTED, null, null,
                "Stock counting started by " + role + " (" + username + ")", null, username, now, locale);

        TripDeliveryWorkflowResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DELIVERY_COUNTING_STARTED.getCode(), new String[]{}, locale));
        response.setWorkflowDto(TripMapper.toWorkflowDto(workflow, trip.getId()));
        response.setTripDto(TripMapper.toDto(trip));
        return response;
    }

    // ============================================================
    // FINISH COUNTING
    // ============================================================

    /**
     * Finish stock counting for a given actor.
     *
     * Flow:
     * 1. Validate
     * 2. Load trip — must be COUNTING_STOCK
     * 3. Load workflow
     * 4. Record counting finish timestamp for actor
     * 5. If countedQuantity provided, set on workflow
     * 6. If both driver and customer have finished → COUNT_COMPLETE
     * 7. Record STOCK_COUNTING_FINISHED event
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripDeliveryWorkflowResponse finishCounting(Long tripId, FinishCountingRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = validator.isFinishCountingRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DELIVERY_FINISH_COUNTING_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load trip
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.COUNTING_STOCK) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_COUNTING_STOCK.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Load workflow (bootstrap if missing during delivery phase)
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        TripDeliveryWorkflow workflow = workflowRepository
                .findWithDetailsByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED)
                .orElse(null);
        if (workflow == null) {
            workflow = workflowBootstrap.ensureWorkflow(trip, now, username, locale);
        }

        // ============================================================
        // STEP 4: Record actor finish timestamp
        // ============================================================
        String role = request.getActorRole().trim().toUpperCase();
        switch (role) {
            case "DRIVER" -> workflow.setDriverCountingFinishedAt(now);
            case "CUSTOMER", "RECEIVER" -> workflow.setCustomerCountingFinishedAt(now);
            default -> {
                return errorResponse(400, "Unknown actor role: " + role);
            }
        }

        // ============================================================
        // STEP 5: Update counted quantity if provided
        // ============================================================
        if (request.getCountedQuantity() != null) {
            workflow.setCountedQuantity(request.getCountedQuantity());
        }
        workflow.setExpectedQuantity(trip.getQuantity() != null ? trip.getQuantity().stripTrailingZeros() : null);

        // ============================================================
        // STEP 6: Both parties finished → COUNT_COMPLETE
        // ============================================================
        boolean driverDone = workflow.getDriverCountingFinishedAt() != null;
        boolean customerDone = workflow.getCustomerCountingFinishedAt() != null;

        workflow.setModifiedAt(now);
        workflow.setModifiedBy(username);
        workflowAuditable.update(workflow, locale, username);

        if (driverDone && customerDone) {
            trip.setStatus(TripStatus.COUNT_COMPLETE);
            trip.setModifiedAt(now);
            trip.setModifiedBy(username);
            tripServiceAuditable.update(trip, locale, username);
            log.info("Trip {} counting complete — both parties finished", tripId);
        }

        // ============================================================
        // STEP 7: Record STOCK_COUNTING_FINISHED event
        // ============================================================
        recordTripEvent(trip, TripEventType.STOCK_COUNTING_FINISHED, null, null,
                "Stock counting finished by " + role + " (" + username + ")", null, username, now, locale);

        TripDeliveryWorkflowResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DELIVERY_COUNTING_FINISHED.getCode(), new String[]{}, locale));
        response.setWorkflowDto(TripMapper.toWorkflowDto(workflow, trip.getId()));
        response.setTripDto(TripMapper.toDto(trip));
        return response;
    }

    // ============================================================
    // SEND DELIVERY OTP
    // ============================================================

    /**
     * Send delivery OTP via chosen channel.
     *
     * Flow:
     * 1. Validate request
     * 2. Load trip — must be COUNT_COMPLETE
     * 3. Load workflow; store channel + recipient
     * 4. Generate OTP, BCrypt hash, persist delivery_otp
     * 5. Transition trip → OTP_PENDING
     * 6. Publish OTP notification with channel in payload
     * 7. Record OTP_SENT event
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripDeliveryWorkflowResponse sendDeliveryOtp(SendDeliveryOtpRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = validator.isSendDeliveryOtpRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DELIVERY_SEND_OTP_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load trip
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(request.getTripId(), EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.COUNT_COMPLETE) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_COUNT_COMPLETE.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Load workflow; store channel + recipient
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        TripDeliveryWorkflow workflow = workflowRepository
                .findByTripIdAndEntityStatusNot(trip.getId(), EntityStatus.DELETED).orElse(null);
        if (workflow == null) {
            workflow = workflowBootstrap.ensureWorkflow(trip, now, username, locale);
        }
        String channel = request.getChannel().trim().toUpperCase();
        workflow.setOtpChannel(channel);
        workflow.setOtpRecipient(request.getRecipientContact());
        workflow.setModifiedAt(now);
        workflow.setModifiedBy(username);
        workflowAuditable.update(workflow, locale, username);

        // ============================================================
        // STEP 4: Generate OTP and persist delivery_otp record
        // ============================================================
        String rawOtp = generateSixDigitOtp();
        String otpHash = bCryptPasswordEncoder.encode(rawOtp);
        LocalDateTime otpExpiry = now.plusMinutes(30);

        DeliveryOtp deliveryOtp = new DeliveryOtp();
        deliveryOtp.setTrip(trip);
        deliveryOtp.setOtpCodeHash(otpHash);
        deliveryOtp.setExpiresAt(otpExpiry);
        deliveryOtp.setSentToUserId(request.getRecipientUserId());
        deliveryOtp.setOtpChannel(channel);
        deliveryOtp.setRecipientContact(request.getRecipientContact());
        deliveryOtp.setSentAt(now);
        deliveryOtp.setEntityStatus(EntityStatus.ACTIVE);
        deliveryOtp.setCreatedAt(now);
        deliveryOtp.setCreatedBy(username);
        deliveryOtpServiceAuditable.create(deliveryOtp, locale, username);
        log.info("Delivery OTP generated for trip {} via {} expires at {}", trip.getId(), channel, otpExpiry);

        // ============================================================
        // STEP 5: Transition trip → OTP_PENDING
        // ============================================================
        trip.setStatus(TripStatus.OTP_PENDING);
        trip.setModifiedAt(now);
        trip.setModifiedBy(username);
        tripServiceAuditable.update(trip, locale, username);

        // ============================================================
        // STEP 6: Publish OTP notification with channel in payload
        // ============================================================
        try {
            Map<String, Object> notificationPayload = new HashMap<>();
            notificationPayload.put("templateCode", "DELIVERY_ARRIVAL_OTP");
            notificationPayload.put("tripId", trip.getId());
            notificationPayload.put("tripNumber", trip.getTripNumber());
            notificationPayload.put("recipientUserId", request.getRecipientUserId());
            notificationPayload.put("recipientContact", request.getRecipientContact());
            notificationPayload.put("channel", channel);
            notificationPayload.put("otp", rawOtp);
            notificationPayload.put("expiresAt", otpExpiry.toString());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notificationPayload);
            log.info("OTP notification enqueued for trip {} via channel {}", trip.getId(), channel);
        } catch (Exception ex) {
            log.error("Failed to publish OTP notification for trip {}: {}", trip.getId(), ex.getMessage());
        }

        // ============================================================
        // STEP 7: Record OTP_SENT event
        // ============================================================
        recordTripEvent(trip, TripEventType.OTP_SENT, null, null,
                "OTP sent via " + channel + " to " + request.getRecipientContact(),
                request.getRecipientUserId(), username, now, locale);

        TripDeliveryWorkflowResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DELIVERY_OTP_SENT_SUCCESS.getCode(), new String[]{}, locale));
        response.setWorkflowDto(TripMapper.toWorkflowDto(workflow, trip.getId()));
        response.setTripDto(TripMapper.toDto(trip));
        return response;
    }

    // ============================================================
    // VERIFY DELIVERY OTP
    // ============================================================

    /**
     * Verify delivery OTP and complete the delivery.
     *
     * Flow:
     * 1. Validate request
     * 2. Load trip — must be OTP_PENDING
     * 3. Verify OTP hash and expiry
     * 4. Mark OTP verified; store delivery notes on workflow
     * 5. Call inventory complete-with-grv
     * 6. Transition trip → DELIVERED
     * 7. Update shipment → DELIVERED
     * 8. Record OTP_VERIFIED + DELIVERED events
     * 9. Publish trip.delivered
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripDeliveryWorkflowResponse verifyDeliveryOtp(VerifyDeliveryOtpRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Basic null check (full validation lives in TripServiceValidator too)
        // ============================================================
        if (request == null || request.getTripId() == null || request.getOtp() == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_DELIVERY_VERIFY_INVALID.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Load trip
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

        Long receiverUserId = request.getReceiverUserId();
        if (receiverUserId == null || receiverUserId < 1) {
            receiverUserId = deliveryOtp.getSentToUserId();
        }
        if (receiverUserId == null || receiverUserId < 1) {
            receiverUserId = trip.getReceiverUserId();
        }
        request.setReceiverUserId(receiverUserId);

        // ============================================================
        // STEP 4: Mark OTP verified; store delivery notes on workflow
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        deliveryOtp.setVerifiedAt(now);
        deliveryOtp.setModifiedAt(now);
        deliveryOtp.setModifiedBy(username);
        deliveryOtpServiceAuditable.update(deliveryOtp, locale, username);

        if (request.getDeliveryNotes() != null && !request.getDeliveryNotes().isBlank()) {
            workflowRepository.findByTripIdAndEntityStatusNot(trip.getId(), EntityStatus.DELETED)
                    .ifPresent(wf -> {
                        wf.setDeliveryNotes(request.getDeliveryNotes().trim());
                        wf.setModifiedAt(now);
                        wf.setModifiedBy(username);
                        workflowRepository.save(wf);
                    });
        }

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
        // STEP 6: Transition trip → DELIVERED
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

        TripDeliveryWorkflowResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_TRIP_DELIVERY_VERIFIED_SUCCESS.getCode(), new String[]{}, locale));
        response.setTripDto(TripMapper.toDto(trip));
        return response;
    }

    // ============================================================
    // START RETURN JOURNEY
    // ============================================================

    /**
     * Start return journey from the delivery destination.
     *
     * Flow:
     * 1. Load trip — must be DELIVERED
     * 2. Load/ensure workflow; set returnInitiatedAt
     * 3. Transition trip → RETURN_IN_TRANSIT
     * 4. Record RETURN_INITIATED event
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripDeliveryWorkflowResponse startReturnJourney(Long tripId, Locale locale, String username) {

        // ============================================================
        // STEP 1: Load trip
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.DELIVERED) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_DELIVERED.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Load workflow; set returnInitiatedAt
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        TripDeliveryWorkflow workflow = workflowRepository
                .findByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED)
                .orElseGet(() -> workflowBootstrap.ensureWorkflow(trip, now, username, locale));

        workflow.setReturnInitiatedAt(now);
        workflow.setModifiedAt(now);
        workflow.setModifiedBy(username);
        workflowAuditable.update(workflow, locale, username);

        // ============================================================
        // STEP 3: Transition trip → RETURN_IN_TRANSIT
        // ============================================================
        trip.setStatus(TripStatus.RETURN_IN_TRANSIT);
        trip.setModifiedAt(now);
        trip.setModifiedBy(username);
        tripServiceAuditable.update(trip, locale, username);

        // ============================================================
        // STEP 4: Record RETURN_INITIATED event
        // ============================================================
        recordTripEvent(trip, TripEventType.RETURN_INITIATED, null, null,
                "Return journey started by " + username, null, username, now, locale);

        TripDeliveryWorkflowResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DELIVERY_RETURN_STARTED.getCode(), new String[]{}, locale));
        response.setWorkflowDto(TripMapper.toWorkflowDto(workflow, trip.getId()));
        response.setTripDto(TripMapper.toDto(trip));
        return response;
    }

    // ============================================================
    // RECORD RETURNS
    // ============================================================

    /**
     * Record return line items.
     *
     * Flow:
     * 1. Validate request
     * 2. Load trip — must be RETURN_IN_TRANSIT (or DELIVERED to allow pre-departure records)
     * 3. Load workflow
     * 4. Persist return lines
     * 5. Record RETURN_RECORDED event
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripDeliveryWorkflowResponse recordReturns(Long tripId, RecordReturnLinesRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate
        // ============================================================
        ValidatorDto validation = validator.isRecordReturnLinesRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400,
                    messageService.getMessage(I18Code.MESSAGE_DELIVERY_RECORD_RETURNS_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Load trip
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.RETURN_IN_TRANSIT && trip.getStatus() != TripStatus.DELIVERED) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_RETURN_IN_TRANSIT.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Load workflow
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        TripDeliveryWorkflow workflow = workflowRepository
                .findByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED)
                .orElseGet(() -> workflowBootstrap.ensureWorkflow(trip, now, username, locale));

        // ============================================================
        // STEP 4: Persist return lines
        // ============================================================
        String role = request.getActorRole() != null ? request.getActorRole().trim().toUpperCase() : null;
        for (ReturnLineItem lineItem : request.getReturnLines()) {
            TripDeliveryReturnLine line = new TripDeliveryReturnLine();
            line.setWorkflow(workflow);
            line.setProductName(lineItem.getProductName());
            line.setQuantity(lineItem.getQuantity() != null ? lineItem.getQuantity() : BigDecimal.ZERO);
            line.setReason(lineItem.getReason());
            line.setRecordedByRole(role);
            line.setEntityStatus(EntityStatus.ACTIVE);
            line.setCreatedAt(now);
            line.setCreatedBy(username);
            workflowAuditable.createReturnLine(line, locale, username);
        }
        log.info("Recorded {} return lines for trip {} by {}", request.getReturnLines().size(), tripId, username);

        // ============================================================
        // STEP 5: Record RETURN_RECORDED event
        // ============================================================
        recordTripEvent(trip, TripEventType.RETURN_RECORDED, null, null,
                request.getReturnLines().size() + " return item(s) recorded by " + username,
                null, username, now, locale);

        TripDeliveryWorkflowResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DELIVERY_RETURNS_RECORDED.getCode(), new String[]{}, locale));
        response.setWorkflowDto(TripMapper.toWorkflowDto(workflow, trip.getId()));
        return response;
    }

    // ============================================================
    // CONFIRM RETURN COMPLETE
    // ============================================================

    /**
     * Confirm the return journey is complete.
     *
     * Flow:
     * 1. Load trip — must be RETURN_IN_TRANSIT
     * 2. Set returnCompletedAt on workflow
     * 3. Transition trip → RETURNED
     * 4. Record RETURN_CONFIRMED event
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TripDeliveryWorkflowResponse confirmReturnComplete(Long tripId, Locale locale, String username) {

        // ============================================================
        // STEP 1: Load trip
        // ============================================================
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return errorResponse(404, messageService.getMessage(I18Code.MESSAGE_TRIP_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        if (trip.getStatus() != TripStatus.RETURN_IN_TRANSIT) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRIP_NOT_RETURN_IN_TRANSIT.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Update workflow returnCompletedAt
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        TripDeliveryWorkflow workflow = workflowRepository
                .findByTripIdAndEntityStatusNot(tripId, EntityStatus.DELETED)
                .orElseGet(() -> workflowBootstrap.ensureWorkflow(trip, now, username, locale));

        workflow.setReturnCompletedAt(now);
        workflow.setModifiedAt(now);
        workflow.setModifiedBy(username);
        workflowAuditable.update(workflow, locale, username);

        // ============================================================
        // STEP 3: Transition trip → RETURNED
        // ============================================================
        trip.setStatus(TripStatus.RETURNED);
        trip.setModifiedAt(now);
        trip.setModifiedBy(username);
        tripServiceAuditable.update(trip, locale, username);

        // ============================================================
        // STEP 4: Record RETURN_CONFIRMED event
        // ============================================================
        recordTripEvent(trip, TripEventType.RETURN_CONFIRMED, null, null,
                "Return journey confirmed complete by " + username, null, username, now, locale);

        TripDeliveryWorkflowResponse response = successResponse(200,
                messageService.getMessage(I18Code.MESSAGE_DELIVERY_RETURN_CONFIRMED.getCode(), new String[]{}, locale));
        response.setWorkflowDto(TripMapper.toWorkflowDto(workflow, trip.getId()));
        response.setTripDto(TripMapper.toDto(trip));
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

    private TripDeliveryWorkflowResponse successResponse(int statusCode, String message) {
        TripDeliveryWorkflowResponse response = new TripDeliveryWorkflowResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private TripDeliveryWorkflowResponse errorResponse(int statusCode, String message) {
        return errorResponse(statusCode, message, new ArrayList<>());
    }

    private TripDeliveryWorkflowResponse errorResponse(int statusCode, String message, List<String> errors) {
        TripDeliveryWorkflowResponse response = new TripDeliveryWorkflowResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
