package projectlx.shipment.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.shipment.management.business.auditable.api.ShipmentServiceAuditable;
import projectlx.shipment.management.business.logic.api.BorderClearanceCaseService;
import projectlx.shipment.management.business.logic.api.ShipmentService;
import projectlx.shipment.management.business.logic.support.CallerOrganizationResolver;
import projectlx.shipment.management.business.logic.support.LogisticsNotificationRecipientResolver;
import projectlx.shipment.management.business.logic.support.ShipmentFleetAllocatorSupport;
import projectlx.shipment.management.business.logic.support.ShipmentMapper;
import projectlx.shipment.management.business.validator.api.ShipmentServiceValidator;
import projectlx.shipment.management.model.Shipment;
import projectlx.shipment.management.clients.FleetManagementServiceClient;
import projectlx.shipment.management.clients.OrganizationManagementServiceClient;
import projectlx.shipment.management.repository.ShipmentRepository;
import projectlx.shipment.management.utils.config.RabbitMQProducerConfig;
import projectlx.shipment.management.utils.dtos.FleetDriverSummaryDto;
import projectlx.shipment.management.utils.enums.I18Code;
import projectlx.shipment.management.utils.enums.ShipmentSourceType;
import projectlx.shipment.management.utils.enums.ShipmentStatus;
import projectlx.shipment.management.utils.requests.AllocateShipmentRequest;
import projectlx.shipment.management.utils.requests.AssignTransportCompanyRequest;
import projectlx.shipment.management.utils.requests.AutoAllocateShipmentFromFleetRequest;
import projectlx.shipment.management.utils.requests.ShipmentMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.UpdateShipmentStatusRequest;
import projectlx.shipment.management.utils.requests.ValidateTransporterAssignmentFeignRequest;
import projectlx.shipment.management.utils.responses.FleetDriverFeignResponse;
import projectlx.shipment.management.utils.responses.ShipmentResponse;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.notifications.LogisticsLifecycleNotificationSupport;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentServiceValidator shipmentServiceValidator;
    private final ShipmentServiceAuditable shipmentServiceAuditable;
    private final ShipmentRepository shipmentRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final ShipmentFleetAllocatorSupport shipmentFleetAllocatorSupport;
    private final RabbitTemplate rabbitTemplate;
    private final MessageService messageService;
    private final LogisticsLifecycleNotificationSupport logisticsLifecycleNotificationSupport;
    private final LogisticsNotificationRecipientResolver recipientResolver;
    private final OrganizationManagementServiceClient organizationManagementServiceClient;
    private final FleetManagementServiceClient fleetManagementServiceClient;
    private final BorderClearanceCaseService borderClearanceCaseService;

    // ============================================================
    // EVENT-DRIVEN CREATION
    // ============================================================

    /**
     * Create a shipment from an inventory.transfer.approved event.
     *
     * Flow:
     * 1. Validate event payload has the minimum required fields
     * 2. Check idempotency — skip if shipment already exists for this transfer
     * 3. Persist the shipment as PENDING_ALLOCATION
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createFromTransferApprovedEvent(Map<String, Object> event, Locale locale) {

        // ============================================================
        // STEP 1: Extract required fields from event payload
        // ============================================================
        if (event == null || !event.containsKey("transferId")) {
            log.warn("inventory.transfer.approved event missing transferId; ignoring.");
            return;
        }

        Long transferId = toLong(event.get("transferId"));
        if (transferId == null) {
            log.warn("inventory.transfer.approved event has null transferId; ignoring.");
            return;
        }

        // ============================================================
        // STEP 2: Idempotency check — skip if shipment already exists
        // ============================================================
        boolean alreadyExists = shipmentRepository.existsByInventoryTransferIdAndEntityStatusNot(
                transferId, EntityStatus.DELETED);
        if (alreadyExists) {
            log.info("Shipment already exists for inventoryTransferId={}; skipping duplicate creation.", transferId);
            return;
        }

        // ============================================================
        // STEP 3: Build and persist the shipment
        // ============================================================
        String shipmentNumber = generateShipmentNumber();
        Long organizationId = toLong(event.get("organizationId"));

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber(shipmentNumber);
        shipment.setOrganizationId(organizationId);
        shipment.setSourceType(ShipmentSourceType.INVENTORY_TRANSFER);
        shipment.setInventoryTransferId(transferId);
        shipment.setFromWarehouseLocationId(toLong(event.get("fromWarehouseLocationId")));
        shipment.setToWarehouseLocationId(toLong(event.get("toWarehouseLocationId")));
        shipment.setFromWarehouseName(toStr(event.get("fromWarehouseName")));
        shipment.setToWarehouseName(toStr(event.get("toWarehouseName")));
        shipment.setProductId(toLong(event.get("productId")));
        shipment.setProductName(toStr(event.get("productName")));
        shipment.setProductCode(toStr(event.get("productCode")));

        Object quantityObj = event.get("quantity");
        BigDecimal quantity = quantityObj != null ? new BigDecimal(quantityObj.toString()) : BigDecimal.ZERO;
        shipment.setQuantity(quantity);

        boolean crossBorder = Boolean.TRUE.equals(event.get("crossBorder"))
                || "true".equalsIgnoreCase(String.valueOf(event.get("crossBorder")));
        shipment.setCrossBorder(crossBorder);

        shipment.setStatus(ShipmentStatus.PENDING_ALLOCATION);
        shipment.setEntityStatus(EntityStatus.ACTIVE);
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setCreatedBy("system");

        Shipment saved = shipmentServiceAuditable.create(shipment, locale, "system");
        log.info("Shipment created from transfer approved event: id={} number={} transferId={}",
                saved.getId(), saved.getShipmentNumber(), transferId);

        // ============================================================
        // STEP 4: Auto-create border clearance case for cross-border shipments
        // ============================================================
        if (saved.isCrossBorder()) {
            borderClearanceCaseService.autoCreateForShipment(
                    saved.getId(), saved.getOrganizationId(), saved.getInventoryTransferId(), null, locale, "system");
        }
    }

    /**
     * Create a shipment from a sales.order.approved event (bought-goods delivery).
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createFromSalesOrderApprovedEvent(Map<String, Object> event, Locale locale) {
        if (event == null || !event.containsKey("salesOrderId")) {
            log.warn("sales.order.approved event missing salesOrderId; ignoring.");
            return;
        }

        Long salesOrderId = toLong(event.get("salesOrderId"));
        if (salesOrderId == null) {
            log.warn("sales.order.approved event has null salesOrderId; ignoring.");
            return;
        }

        boolean alreadyExists = shipmentRepository.existsBySalesOrderIdAndEntityStatusNot(
                salesOrderId, EntityStatus.DELETED);
        if (alreadyExists) {
            log.info("Shipment already exists for salesOrderId={}; skipping duplicate creation.", salesOrderId);
            return;
        }

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber(generateShipmentNumber());
        shipment.setOrganizationId(toLong(event.get("organizationId")));
        shipment.setSourceType(ShipmentSourceType.SALES_ORDER);
        shipment.setSalesOrderId(salesOrderId);
        shipment.setPurchaseOrderId(toLong(event.get("purchaseOrderId")));
        shipment.setCustomerOrganizationId(toLong(event.get("customerOrganizationId")));
        shipment.setFromWarehouseLocationId(toLong(event.get("fromWarehouseLocationId")));
        shipment.setToWarehouseLocationId(toLong(event.get("toWarehouseLocationId")));
        shipment.setFromWarehouseName(toStr(event.get("fromWarehouseName")));
        shipment.setToWarehouseName(toStr(event.get("toWarehouseName")));
        shipment.setProductId(toLong(event.get("productId")));
        shipment.setProductName(toStr(event.get("productName")));
        shipment.setProductCode(toStr(event.get("productCode")));

        Object quantityObj = event.get("quantity");
        BigDecimal quantity = quantityObj != null ? new BigDecimal(quantityObj.toString()) : BigDecimal.ZERO;
        shipment.setQuantity(quantity);

        boolean crossBorder = Boolean.TRUE.equals(event.get("crossBorder"))
                || "true".equalsIgnoreCase(String.valueOf(event.get("crossBorder")));
        shipment.setCrossBorder(crossBorder);

        shipment.setStatus(ShipmentStatus.PENDING_ALLOCATION);
        shipment.setEntityStatus(EntityStatus.ACTIVE);
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setCreatedBy("system");

        Shipment saved = shipmentServiceAuditable.create(shipment, locale, "system");
        log.info("Shipment created from sales order approved event: id={} number={} salesOrderId={}",
                saved.getId(), saved.getShipmentNumber(), salesOrderId);

        if (saved.isCrossBorder()) {
            borderClearanceCaseService.autoCreateForShipment(
                    saved.getId(), saved.getOrganizationId(), null, saved.getSalesOrderId(), locale, "system");
        }
    }

    /**
     * Create a shipment from a cross.dock.dispatch.created event.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createFromCrossDockDispatchCreatedEvent(Map<String, Object> event, Locale locale) {

        // ============================================================
        // STEP 1: Extract required fields from event payload
        // ============================================================
        if (event == null || !event.containsKey("dispatchId")) {
            log.warn("cross.dock.dispatch.created event missing dispatchId; ignoring.");
            return;
        }

        Long dispatchId = toLong(event.get("dispatchId"));
        if (dispatchId == null) {
            log.warn("cross.dock.dispatch.created event has null dispatchId; ignoring.");
            return;
        }

        // ============================================================
        // STEP 2: Idempotency check — skip if shipment already exists
        // ============================================================
        boolean alreadyExists = shipmentRepository.existsByCrossDockDispatchIdAndEntityStatusNot(
                dispatchId, EntityStatus.DELETED);
        if (alreadyExists) {
            log.info("Shipment already exists for crossDockDispatchId={}; skipping duplicate creation.", dispatchId);
            return;
        }

        // ============================================================
        // STEP 3: Build and persist the shipment
        // ============================================================
        Long organizationId = toLong(event.get("organizationId"));
        String fromLocationLabel = toStr(event.get("fromLocationLabel"));
        String toLocationLabel = toStr(event.get("toLocationLabel"));

        Object quantityObj = event.get("quantity");
        BigDecimal quantity = quantityObj != null ? new BigDecimal(quantityObj.toString()) : BigDecimal.ZERO;

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber(generateShipmentNumber());
        shipment.setOrganizationId(organizationId);
        shipment.setSourceType(ShipmentSourceType.CROSS_DOCK_DISPATCH);
        shipment.setCrossDockDispatchId(dispatchId);
        shipment.setFromWarehouseName(fromLocationLabel);
        shipment.setToWarehouseName(toLocationLabel);
        shipment.setProductCode(toStr(event.get("productCode")));
        shipment.setQuantity(quantity);
        shipment.setStatus(ShipmentStatus.PENDING_ALLOCATION);
        shipment.setEntityStatus(EntityStatus.ACTIVE);
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setCreatedBy("system");

        Shipment saved = shipmentServiceAuditable.create(shipment, locale, "system");
        log.info("Shipment created from cross.dock.dispatch.created event: id={} number={} dispatchId={}",
                saved.getId(), saved.getShipmentNumber(), dispatchId);
    }

    // ============================================================
    // QUERIES
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse findById(Long id, Locale locale, String username) {
        if (isSystemUser(username)) {
            Shipment shipment = shipmentRepository.findByIdAndEntityStatusNot(
                    id, EntityStatus.DELETED).orElse(null);
            if (shipment == null) {
                return errorResponse(404, messageService.getMessage(
                        I18Code.MESSAGE_SHIPMENT_NOT_FOUND.getCode(), new String[]{}, locale));
            }
            ShipmentResponse response = successResponse(200, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_FIND_SUCCESS.getCode(), new String[]{}, locale));
            response.setShipmentDto(ShipmentMapper.toDto(shipment));
            return response;
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        Shipment shipment = shipmentRepository.findByIdAndEntityStatusNot(
                id, EntityStatus.DELETED).orElse(null);
        if (shipment == null || !canAccessShipment(shipment, organizationId)) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        ShipmentResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_SHIPMENT_FIND_SUCCESS.getCode(), new String[]{}, locale));
        response.setShipmentDto(ShipmentMapper.toDto(shipment));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse findByMultipleFilters(ShipmentMultipleFiltersRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate the filter request
        // ============================================================
        ValidatorDto validation = shipmentServiceValidator.isShipmentMultipleFiltersRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale), validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Resolve caller's organisation or honour explicit org filter
        // ============================================================
        Long resolvedOrgId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (resolvedOrgId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        Long orgId = (request != null && request.getOrganizationId() != null)
                ? request.getOrganizationId()
                : resolvedOrgId;

        // ============================================================
        // STEP 3: Load shipments owned by or assigned to the caller
        // ============================================================
        Map<Long, Shipment> merged = new LinkedHashMap<>();
        for (Shipment shipment : shipmentRepository.findByOrganizationIdAndEntityStatusNotOrderByIdDesc(
                orgId, EntityStatus.DELETED)) {
            merged.put(shipment.getId(), shipment);
        }
        for (Shipment shipment : shipmentRepository.findByTransportCompanyOrganizationIdAndEntityStatusNotOrderByIdDesc(
                resolvedOrgId, EntityStatus.DELETED)) {
            merged.put(shipment.getId(), shipment);
        }
        List<Shipment> all = new ArrayList<>(merged.values());

        if (request != null) {
            ShipmentStatus filterStatus = resolveStatusFilter(request.getStatus());
            if (filterStatus != null) {
                all = all.stream().filter(s -> s.getStatus() == filterStatus).collect(Collectors.toList());
            }
            if (request.getInventoryTransferId() != null) {
                all = all.stream()
                        .filter(s -> request.getInventoryTransferId().equals(s.getInventoryTransferId()))
                        .collect(Collectors.toList());
            }
            if (request.getSearch() != null && !request.getSearch().isBlank()) {
                String term = request.getSearch().trim().toLowerCase();
                all = all.stream().filter(s ->
                        (s.getShipmentNumber() != null && s.getShipmentNumber().toLowerCase().contains(term))
                                || (s.getProductName() != null && s.getProductName().toLowerCase().contains(term))
                                || (s.getProductCode() != null && s.getProductCode().toLowerCase().contains(term))
                ).collect(Collectors.toList());
            }
        }

        ShipmentResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_SHIPMENT_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setShipmentDtoList(all.stream().map(ShipmentMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse findByTransferId(Long transferId, Locale locale, String username) {
        if (isSystemUser(username)) {
            Shipment shipment = shipmentRepository.findByInventoryTransferIdAndEntityStatusNot(
                    transferId, EntityStatus.DELETED).orElse(null);
            if (shipment == null) {
                return errorResponse(404, messageService.getMessage(
                        I18Code.MESSAGE_SHIPMENT_NOT_FOUND.getCode(), new String[]{}, locale));
            }
            ShipmentResponse response = successResponse(200, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_FIND_SUCCESS.getCode(), new String[]{}, locale));
            response.setShipmentDto(ShipmentMapper.toDto(shipment));
            return response;
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        Shipment shipment = shipmentRepository.findByInventoryTransferIdAndEntityStatusNot(
                transferId, EntityStatus.DELETED).orElse(null);
        if (shipment == null || !canAccessShipment(shipment, organizationId)) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        ShipmentResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_SHIPMENT_FIND_SUCCESS.getCode(), new String[]{}, locale));
        response.setShipmentDto(ShipmentMapper.toDto(shipment));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse findBySalesOrderId(Long salesOrderId, Locale locale, String username) {
        if (isSystemUser(username)) {
            Shipment shipment = shipmentRepository.findBySalesOrderIdAndEntityStatusNot(
                    salesOrderId, EntityStatus.DELETED).orElse(null);
            if (shipment == null) {
                return errorResponse(404, messageService.getMessage(
                        I18Code.MESSAGE_SHIPMENT_NOT_FOUND.getCode(), new String[]{}, locale));
            }
            ShipmentResponse response = successResponse(200, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_FIND_SUCCESS.getCode(), new String[]{}, locale));
            response.setShipmentDto(ShipmentMapper.toDto(shipment));
            return response;
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        Shipment shipment = shipmentRepository.findBySalesOrderIdAndEntityStatusNot(
                salesOrderId, EntityStatus.DELETED).orElse(null);
        if (shipment == null || !canAccessShipment(shipment, organizationId)) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        ShipmentResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_SHIPMENT_FIND_SUCCESS.getCode(), new String[]{}, locale));
        response.setShipmentDto(ShipmentMapper.toDto(shipment));
        return response;
    }

    // ============================================================
    // TRANSPORT COMPANY ASSIGNMENT (shipper dispatch)
    // ============================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShipmentResponse assignTransportCompany(AssignTransportCompanyRequest request, Locale locale, String username) {

        ValidatorDto validation = shipmentServiceValidator.isAssignTransportCompanyRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_ALLOCATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        Shipment shipment = shipmentRepository.findByIdAndEntityStatusNot(
                request.getShipmentId(), EntityStatus.DELETED).orElse(null);
        if (shipment == null || !organizationId.equals(shipment.getOrganizationId())) {
            return errorResponse(403, messageService.getMessage(
                    I18Code.MESSAGE_TRANSPORT_COMPANY_ASSIGN_FORBIDDEN.getCode(), new String[]{}, locale));
        }

        if (shipment.getStatus() != ShipmentStatus.PENDING_ALLOCATION) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_ALREADY_ALLOCATED.getCode(), new String[]{}, locale));
        }

        if (!isTransporterAssignmentValid(shipment.getOrganizationId(), request.getTransportCompanyOrganizationId(), locale)) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_TRANSPORT_COMPANY_INVALID.getCode(), new String[]{}, locale));
        }

        String transportCompanyName = resolveTransportCompanyName(request.getTransportCompanyOrganizationId(), locale);
        shipment.setTransportCompanyOrganizationId(request.getTransportCompanyOrganizationId());
        shipment.setTransportCompanyName(transportCompanyName);
        shipment.setStatus(ShipmentStatus.PENDING_FLEET_ALLOCATION);
        shipment.setModifiedAt(LocalDateTime.now());
        shipment.setModifiedBy(username);
        Shipment saved = shipmentServiceAuditable.update(shipment, locale, username);
        log.info("Transport company assigned to shipment: id={} transportCompanyOrgId={}",
                saved.getId(), saved.getTransportCompanyOrganizationId());

        ShipmentResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_TRANSPORT_COMPANY_ASSIGN_SUCCESS.getCode(), new String[]{}, locale));
        response.setShipmentDto(ShipmentMapper.toDto(saved));
        return response;
    }

    // ============================================================
    // FLEET ALLOCATION (assigned transport company)
    // ============================================================

    /**
     * Allocate a fleet driver and asset to a shipment.
     *
     * Flow:
     * 1. Validate allocation request fields
     * 2. Resolve caller's organisation and load the shipment
     * 3. Guard: shipment must be PENDING_ALLOCATION
     * 4. Apply allocation and transition status to ALLOCATED
     * 5. Publish shipment.allocated event
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShipmentResponse allocateFleet(AllocateShipmentRequest request, Locale locale, String username) {
        return doAllocateFleet(request, locale, username, true);
    }

    private ShipmentResponse doAllocateFleet(AllocateShipmentRequest request, Locale locale, String username,
                                             boolean requireAllocatorEligibility) {

        // ============================================================
        // STEP 1: Validate the request
        // ============================================================
        ValidatorDto validation = shipmentServiceValidator.isAllocateShipmentRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_ALLOCATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        // ============================================================
        // STEP 2: Resolve caller's organisation and load the shipment
        // ============================================================
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
        }

        Shipment shipment = shipmentRepository.findByIdAndEntityStatusNot(
                request.getShipmentId(), EntityStatus.DELETED).orElse(null);
        if (shipment == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Guard — must be awaiting fleet allocation
        // ============================================================
        if (shipment.getStatus() != ShipmentStatus.PENDING_FLEET_ALLOCATION) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_PENDING_FLEET_ALLOCATION.getCode(), new String[]{}, locale));
        }

        Long transportCompanyOrganizationId = shipment.getTransportCompanyOrganizationId();
        boolean isAssignedTransportCompany = transportCompanyOrganizationId != null
                && transportCompanyOrganizationId.equals(organizationId);
        boolean isShipper = organizationId.equals(shipment.getOrganizationId());
        if (!isAssignedTransportCompany && !isShipper) {
            return errorResponse(403, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_FLEET_ALLOCATE_FORBIDDEN.getCode(), new String[]{}, locale));
        }

        if (requireAllocatorEligibility) {
            Optional<String> allocatorError = shipmentFleetAllocatorSupport.validateCallerCanAllocate(username, locale);
            if (allocatorError.isPresent()) {
                return errorResponse(403, messageService.getMessage(
                        I18Code.MESSAGE_SHIPMENT_FLEET_ALLOCATOR_REQUIRED.getCode(), new String[]{}, locale));
            }
        }

        if (!isFleetDriverEligibleForAllocation(request.getFleetDriverId(), shipment, organizationId, locale)) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_FLEET_DRIVER_ORG_MISMATCH.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 4: Apply allocation
        // ============================================================
        shipment.setFleetDriverId(request.getFleetDriverId());
        shipment.setFleetAssetId(request.getFleetAssetId());
        shipment.setStatus(ShipmentStatus.ALLOCATED);
        shipment.setModifiedAt(LocalDateTime.now());
        shipment.setModifiedBy(username);
        Shipment saved = shipmentServiceAuditable.update(shipment, locale, username);
        log.info("Shipment allocated: id={} driver={} asset={}", saved.getId(),
                saved.getFleetDriverId(), saved.getFleetAssetId());

        // ============================================================
        // STEP 5: Publish shipment.allocated event
        // ============================================================
        try {
            Map<String, Object> event = buildAllocatedEvent(saved);
            rabbitTemplate.convertAndSend(RabbitMQProducerConfig.SHIPMENT_EXCHANGE,
                    RabbitMQProducerConfig.SHIPMENT_ALLOCATED_ROUTING_KEY, event);
            log.info("Published shipment.allocated event for shipmentId={}", saved.getId());
        } catch (Exception ex) {
            log.error("Failed to publish shipment.allocated event for shipmentId={}: {}",
                    saved.getId(), ex.getMessage(), ex);
        }

        // ============================================================
        // STEP 6: Send logistics lifecycle notifications (non-blocking)
        // ============================================================
        sendShipmentAllocatedNotification(saved, locale, username);

        ShipmentResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_SHIPMENT_ALLOCATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setShipmentDto(ShipmentMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShipmentResponse autoAllocateFromFleet(AutoAllocateShipmentFromFleetRequest request, Locale locale, String username) {
        if (request == null
                || request.getFleetAssetId() == null
                || request.getFleetAssetId() < 1
                || request.getFleetDriverId() == null
                || request.getFleetDriverId() < 1
                || request.getAssetOrganizationId() == null
                || request.getAssetOrganizationId() < 1) {
            ShipmentResponse response = successResponse(200, "No fleet assignment to sync.");
            return response;
        }

        Long transportCompanyId = resolveTransportCompanyIdForFleetAsset(
                request.getOwnershipType(), request.getAssetOrganizationId(), request.getContractedTransporterOrganizationId());
        if (transportCompanyId == null) {
            ShipmentResponse response = successResponse(200, "No transport company context for fleet auto-allocation.");
            return response;
        }

        List<Shipment> pending = shipmentRepository
                .findByOrganizationIdAndStatusAndTransportCompanyOrganizationIdAndEntityStatusNotOrderByIdAsc(
                        request.getAssetOrganizationId(),
                        ShipmentStatus.PENDING_FLEET_ALLOCATION,
                        transportCompanyId,
                        EntityStatus.DELETED);
        if (pending.isEmpty()) {
            ShipmentResponse response = successResponse(200, "No shipment awaiting fleet allocation.");
            return response;
        }

        Shipment target = pending.get(0);
        AllocateShipmentRequest allocateRequest = new AllocateShipmentRequest();
        allocateRequest.setShipmentId(target.getId());
        allocateRequest.setFleetDriverId(request.getFleetDriverId());
        allocateRequest.setFleetAssetId(request.getFleetAssetId());
        return doAllocateFleet(allocateRequest, locale, username, false);
    }

    // ============================================================
    // STATUS UPDATES (from trip-tracking system calls)
    // ============================================================

    /**
     * Update a shipment's status from a system/internal call (e.g. trip service).
     * Allowed transitions: ALLOCATED → IN_TRANSIT → ARRIVED_PENDING_OTP → DELIVERED
     *
     * Flow:
     * 1. Validate the request
     * 2. Load the shipment by id
     * 3. Validate the transition is legal
     * 4. Apply and persist
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShipmentResponse updateStatus(UpdateShipmentStatusRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate the request
        // ============================================================
        ValidatorDto validation = shipmentServiceValidator.isUpdateShipmentStatusRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_ALLOCATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        ShipmentStatus newStatus;
        try {
            newStatus = ShipmentStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_INVALID_STATUS_TRANSITION.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 2: Load the shipment
        // ============================================================
        Shipment shipment = shipmentRepository.findByIdAndEntityStatusNot(
                request.getShipmentId(), EntityStatus.DELETED).orElse(null);
        if (shipment == null) {
            return errorResponse(404, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 3: Validate the transition is legal
        // ============================================================
        if (!isValidTransition(shipment.getStatus(), newStatus)) {
            return errorResponse(400, messageService.getMessage(
                    I18Code.MESSAGE_SHIPMENT_INVALID_STATUS_TRANSITION.getCode(), new String[]{}, locale));
        }

        // ============================================================
        // STEP 4: Apply and persist
        // ============================================================
        shipment.setStatus(newStatus);
        if (request.getTripId() != null) {
            shipment.setTripId(request.getTripId());
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            shipment.setNotes(request.getNotes());
        }
        shipment.setModifiedAt(LocalDateTime.now());
        shipment.setModifiedBy(username);
        Shipment saved = shipmentServiceAuditable.update(shipment, locale, username);
        log.info("Shipment status updated: id={} newStatus={} by={}",
                saved.getId(), saved.getStatus(), username);

        if (request.getTripId() != null) {
            borderClearanceCaseService.linkTripId(saved.getId(), request.getTripId(), locale, username);
        }

        ShipmentResponse response = successResponse(200, messageService.getMessage(
                I18Code.MESSAGE_SHIPMENT_STATUS_UPDATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setShipmentDto(ShipmentMapper.toDto(saved));
        return response;
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private void sendShipmentAllocatedNotification(Shipment shipment, Locale locale, String performedBy) {
        try {
            OrganizationDto org = recipientResolver.resolveOrganization(shipment.getOrganizationId(), locale);
            List<UserDto> fleetManagers = recipientResolver.resolveFleetManagers(shipment.getOrganizationId(), locale);
            LogisticsNotificationRecipientResolver.DriverContact driver =
                    recipientResolver.resolveDriverContact(shipment.getFleetDriverId(), locale);

            String orgEmail = org != null ? org.getEmail() : null;
            String orgPhone = org != null ? org.getPhoneNumber() : null;
            String contactEmail = org != null ? org.getContactPersonEmail() : null;
            String contactPhone = org != null ? org.getContactPersonPhoneNumber() : null;
            String orgName = org != null ? org.getName() : "";
            String contactName = org != null
                    ? buildOrgContactName(org.getContactPersonFirstName(), org.getContactPersonLastName(), orgName)
                    : "";

            logisticsLifecycleNotificationSupport.notifyShipmentAllocated(
                    shipment.getOrganizationId(),
                    orgEmail, orgPhone, contactEmail, contactPhone, orgName, contactName,
                    fleetManagers,
                    driver.email(), driver.phone(), driver.name(),
                    shipment.getShipmentNumber(),
                    shipment.getFromWarehouseName(),
                    shipment.getToWarehouseName(),
                    shipment.getProductName(),
                    shipment.getQuantity() != null ? shipment.getQuantity().toPlainString() : "0",
                    performedBy);
        } catch (Exception ex) {
            log.error("Failed to send shipment-allocated notification for shipmentId={}: {}",
                    shipment.getId(), ex.getMessage());
        }
    }

    private static String buildOrgContactName(String firstName, String lastName, String fallback) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? fallback : name;
    }

    private boolean isValidTransition(ShipmentStatus current, ShipmentStatus next) {
        return switch (current) {
            case ALLOCATED -> next == ShipmentStatus.IN_TRANSIT || next == ShipmentStatus.CANCELLED;
            case IN_TRANSIT -> next == ShipmentStatus.ARRIVED_PENDING_OTP || next == ShipmentStatus.CANCELLED;
            case ARRIVED_PENDING_OTP -> next == ShipmentStatus.DELIVERED || next == ShipmentStatus.CANCELLED;
            default -> false;
        };
    }

    private Map<String, Object> buildAllocatedEvent(Shipment shipment) {
        Map<String, Object> event = new HashMap<>();
        event.put("shipmentId", shipment.getId());
        event.put("shipmentNumber", shipment.getShipmentNumber());
        event.put("organizationId", shipment.getOrganizationId());
        event.put("inventoryTransferId", shipment.getInventoryTransferId());
        event.put("salesOrderId", shipment.getSalesOrderId());
        event.put("fleetDriverId", shipment.getFleetDriverId());
        event.put("fleetAssetId", shipment.getFleetAssetId());
        event.put("status", shipment.getStatus().name());
        return event;
    }

    private String generateShipmentNumber() {
        return "SHP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private ShipmentResponse successResponse(int statusCode, String message) {
        ShipmentResponse response = new ShipmentResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private ShipmentResponse errorResponse(int statusCode, String message) {
        return errorResponse(statusCode, message, new ArrayList<>());
    }

    private ShipmentResponse errorResponse(int statusCode, String message, List<String> errors) {
        ShipmentResponse response = new ShipmentResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Accepts portal aliases (e.g. PENDING) as well as canonical enum names.
     * Returns null when the filter should be ignored.
     */
    private ShipmentStatus resolveStatusFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING" -> ShipmentStatus.PENDING_ALLOCATION;
            case "PENDING_FLEET" -> ShipmentStatus.PENDING_FLEET_ALLOCATION;
            case "ARRIVED" -> ShipmentStatus.ARRIVED_PENDING_OTP;
            default -> {
                try {
                    yield ShipmentStatus.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    log.warn("Unknown shipment status filter: {}", raw);
                    yield null;
                }
            }
        };
    }

    private static String toStr(Object value) {
        return value != null ? value.toString() : null;
    }

    private boolean isSystemUser(String username) {
        return username != null && "SYSTEM".equalsIgnoreCase(username);
    }

    private boolean canAccessShipment(Shipment shipment, Long callerOrganizationId) {
        if (shipment == null || callerOrganizationId == null) {
            return false;
        }
        if (callerOrganizationId.equals(shipment.getOrganizationId())) {
            return true;
        }
        return callerOrganizationId.equals(shipment.getTransportCompanyOrganizationId());
    }

    private boolean isTransporterAssignmentValid(Long shipperOrganizationId, Long transportCompanyOrganizationId,
                                                 Locale locale) {
        ValidateTransporterAssignmentFeignRequest request = new ValidateTransporterAssignmentFeignRequest();
        request.setShipperOrganizationId(shipperOrganizationId);
        request.setTransportCompanyOrganizationId(transportCompanyOrganizationId);
        try {
            OrganizationResponse response = organizationManagementServiceClient.validateTransporterAssignment(request, locale);
            return response != null && response.isSuccess();
        } catch (Exception ex) {
            log.warn("Failed to validate transporter assignment shipperOrgId={} transportCompanyOrgId={}: {}",
                    shipperOrganizationId, transportCompanyOrganizationId, ex.getMessage());
            return false;
        }
    }

    private String resolveTransportCompanyName(Long transportCompanyOrganizationId, Locale locale) {
        if (transportCompanyOrganizationId == null) {
            return null;
        }
        try {
            OrganizationResponse response = organizationManagementServiceClient.findById(transportCompanyOrganizationId, locale);
            if (response != null && response.isSuccess() && response.getOrganizationDto() != null) {
                return response.getOrganizationDto().getName();
            }
        } catch (Exception ex) {
            log.warn("Failed to resolve transport company name for orgId={}: {}",
                    transportCompanyOrganizationId, ex.getMessage());
        }
        return "Transport company #" + transportCompanyOrganizationId;
    }

    private boolean isFleetDriverEligibleForAllocation(Long fleetDriverId, Shipment shipment, Long callerOrgId, Locale locale) {
        if (fleetDriverId == null || callerOrgId == null) {
            return false;
        }
        try {
            FleetDriverFeignResponse response = fleetManagementServiceClient.findFleetDriverById(fleetDriverId, locale);
            FleetDriverSummaryDto driver = response != null ? response.getFleetDriverDto() : null;
            if (driver == null || driver.getOrganizationId() == null) {
                return false;
            }
            Long driverOrgId = driver.getOrganizationId();
            if (callerOrgId.equals(driverOrgId)) {
                return true;
            }
            Long transportCompanyId = shipment.getTransportCompanyOrganizationId();
            return callerOrgId.equals(shipment.getOrganizationId())
                    && transportCompanyId != null
                    && transportCompanyId.equals(driverOrgId);
        } catch (Exception ex) {
            log.warn("Failed to resolve fleet driver {} for allocation eligibility: {}", fleetDriverId, ex.getMessage());
            return false;
        }
    }

    private Long resolveTransportCompanyIdForFleetAsset(String ownershipTypeRaw,
                                                        Long assetOrganizationId,
                                                        Long contractedTransporterOrganizationId) {
        String ownership = ownershipTypeRaw == null ? "" : ownershipTypeRaw.trim().toUpperCase();
        if ("CONTRACTED".equals(ownership)) {
            return contractedTransporterOrganizationId;
        }
        return assetOrganizationId;
    }
}
