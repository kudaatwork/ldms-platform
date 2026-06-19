package projectlx.inventory.management.business.logic.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.DocumentException;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.inventory.management.business.auditable.api.GoodsReceivedVoucherServiceAuditable;
import projectlx.inventory.management.business.auditable.api.InventoryTransferServiceAuditable;
import projectlx.inventory.management.business.logic.api.IdempotencyService;
import projectlx.inventory.management.business.logic.api.InventoryItemService;
import projectlx.inventory.management.business.logic.api.InventoryTransferService;
import projectlx.inventory.management.business.logic.api.LogisticsRouteStopService;
import projectlx.inventory.management.model.RouteStopContextType;
import projectlx.inventory.management.business.logic.support.ProcurementApproverSupport;
import projectlx.inventory.management.business.logic.support.StockTransferSupport;
import projectlx.inventory.management.business.logic.support.TransitWarehouseSupport;
import projectlx.inventory.management.business.logic.support.TransferDispatchSupport;
import projectlx.inventory.management.business.validator.api.InventoryTransferServiceValidator;
import projectlx.inventory.management.clients.UserManagementServiceClient;
import projectlx.inventory.management.model.GoodsReceivedVoucher;
import projectlx.inventory.management.model.GrvStatus;
import projectlx.inventory.management.model.IdempotencyKey;
import projectlx.inventory.management.model.IdempotencyOperation;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.InventoryTransfer;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.ReferenceDocumentType;
import projectlx.inventory.management.model.RouteStopType;
import projectlx.inventory.management.model.StockTransactionHistory;
import projectlx.inventory.management.model.TransferStatus;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.repository.GoodsReceivedVoucherRepository;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.InventoryTransferRepository;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.StockTransactionHistoryRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.specification.InventoryTransferSpecification;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.dtos.InventoryTransferDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.CreateOrUpdateStockRequest;
import projectlx.inventory.management.utils.requests.EditInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.RouteStopRequest;
import projectlx.inventory.management.utils.requests.InventoryTransferMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.NotificationRequest;
import projectlx.inventory.management.utils.responses.InventoryItemResponse;
import projectlx.inventory.management.utils.responses.InventoryTransferResponse;
import projectlx.inventory.management.utils.responses.LogisticsRouteStopResponse;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryTransferServiceImpl implements InventoryTransferService {

    private final InventoryTransferRepository repository;
    private final ProductRepository productRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final InventoryTransferServiceAuditable auditable;
    private final InventoryItemService inventoryItemService;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockTransactionHistoryRepository stockTransactionHistoryRepository;
    private final InventoryTransferServiceValidator inventoryTransferServiceValidator;
    private final ModelMapper modelMapper;
    private final MessageService messageService;
    private final RabbitTemplate rabbitTemplate;
    private final IdempotencyService idempotencyService;
    private final UserManagementServiceClient userManagementServiceClient;
    private final ProcurementApproverSupport procurementApproverSupport;
    private final TransferDispatchSupport transferDispatchSupport;
    private final TransitWarehouseSupport transitWarehouseSupport;
    private final StockTransferSupport stockTransferSupport;
    private final GoodsReceivedVoucherServiceAuditable goodsReceivedVoucherServiceAuditable;
    private final GoodsReceivedVoucherRepository goodsReceivedVoucherRepository;
    private final LogisticsRouteStopService logisticsRouteStopService;

    private static final String[] HEADERS = {"ID", "TRANSFER_NUMBER", "PRODUCT_ID", "FROM_LOCATION_ID", "TO_LOCATION_ID", "QUANTITY", "STATUS", "REFERENCE"};
    private static final String[] CSV_HEADERS = {
            "PRODUCT_ID", "FROM_LOCATION_ID", "TO_LOCATION_ID", "QUANTITY", "REFERENCE", "CREATED_BY_USER_ID"
    };

    /**
     * Creates a new inventory transfer in REQUESTED status.
     * Auto-creates inventory items if they don't exist at source or destination.
     * No stock movements occur at this stage.
     */
    @Override
    @Transactional
    public InventoryTransferResponse create(CreateInventoryTransferRequest request, Locale locale, String username) {
        String message;

        ValidatorDto validatorDto = inventoryTransferServiceValidator
                .isCreateInventoryTransferRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<Product> productOpt = productRepository
                .findByIdAndEntityStatusNot(request.getProductId(), EntityStatus.DELETED);

        if (productOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_PRODUCT_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Optional<WarehouseLocation> fromLocationOpt = warehouseLocationRepository
                .findByIdAndEntityStatusNot(request.getFromLocationId(), EntityStatus.DELETED);

        if (fromLocationOpt.isEmpty()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INVENTORY_TRANSFER_FROM_LOCATION_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Optional<WarehouseLocation> toLocationOpt = warehouseLocationRepository
                .findByIdAndEntityStatusNot(request.getToLocationId(), EntityStatus.DELETED);

        if (toLocationOpt.isEmpty()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INVENTORY_TRANSFER_TO_LOCATION_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        // AUTO-CREATE: Verify or create InventoryItem at SOURCE warehouse
        Optional<InventoryItem> fromItemOpt = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        request.getProductId(), request.getFromLocationId(), EntityStatus.DELETED);

        if (fromItemOpt.isEmpty()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INVENTORY_TRANSFER_TO_LOCATION_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryItem fromItem = fromItemOpt.get();

        // AUTO-CREATE: Verify or create InventoryItem at DESTINATION warehouse
        Optional<InventoryItem> toItemOpt = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        request.getProductId(), request.getToLocationId(), EntityStatus.DELETED);

        if (toItemOpt.isEmpty()) {
            log.info("Auto-creating inventory item for product {} at destination warehouse {}",
                    request.getProductId(), request.getToLocationId());
            InventoryItem toItem = createInventoryItemFromSource(productOpt.get(), toLocationOpt.get(), fromItem, request.getCreatedByUserId());
            inventoryItemRepository.save(toItem);
        }

        // Create transfer record in REQUESTED status
        InventoryTransfer transfer = new InventoryTransfer();
        transfer.setTransferNumber(generateTransferNumber());
        transfer.setProduct(productOpt.get());
        transfer.setFromLocation(fromLocationOpt.get());
        transfer.setToLocation(toLocationOpt.get());
        transfer.setQuantity(request.getQuantity());
        transfer.setUnitCost(fromItem.getAverageCost());
        transfer.setStatus(TransferStatus.REQUESTED);  // FIXED: Start at REQUESTED
        transfer.setReference(request.getReference());
        transfer.setCrossBorder(Boolean.TRUE.equals(request.getCrossBorder()));
        transfer.setCreatedByUserId(request.getCreatedByUserId());

        InventoryTransfer savedTransfer = auditable.create(transfer, locale, username);
        savedTransfer.setProduct(productOpt.get());
        savedTransfer.setFromLocation(fromLocationOpt.get());
        savedTransfer.setToLocation(toLocationOpt.get());

        persistTransferRouteStops(savedTransfer, request.getRouteStops(), locale, username);

        sendInventoryTransferCreatedInternal(savedTransfer);

        InventoryTransferDto dto = mapToDto(savedTransfer);
        message = messageService.getMessage(
                I18Code.MESSAGE_INVENTORY_TRANSFER_CREATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(201, true, message, dto, null, null);
    }

    /**
     * Helper method to create a new inventory item at destination by copying fields from source item
     */
    private InventoryItem createInventoryItemFromSource(Product product, WarehouseLocation location,
                                                        InventoryItem sourceItem, Long createdByUserId) {
        InventoryItem item = new InventoryItem();
        item.setProduct(product);
        item.setWarehouseLocation(location);

        // Copy important fields from source item
        item.setSupplierId(sourceItem.getSupplierId());
        item.setBatchLot(sourceItem.getBatchLot());
        item.setSerialNumber(sourceItem.getSerialNumber());
        item.setExpiresAt(sourceItem.getExpiresAt());
        item.setMinStockLevel(sourceItem.getMinStockLevel());
        item.setReorderQuantity(sourceItem.getReorderQuantity());
        item.setLastPurchaseCost(sourceItem.getLastPurchaseCost());
        item.setCreatedByUserId(createdByUserId);

        // Initialize stock and cost fields to zero (will be updated on transfer completion)
        item.setQuantity(BigDecimal.ZERO);
        item.setCurrentStock(BigDecimal.ZERO);
        item.setReservedQuantity(BigDecimal.ZERO);
        item.setTotalCost(BigDecimal.ZERO);
        item.setAverageCost(sourceItem.getAverageCost());
        item.setUnitCost(sourceItem.getUnitCost());
        item.setEntityStatus(EntityStatus.ACTIVE);

        return item;
    }

    /**
     * Approves a REQUESTED transfer and moves it to APPROVED status.
     * This indicates the source warehouse has reviewed and authorized the transfer.
     * No stock movements occur at this stage.
     */
    @Transactional
    public InventoryTransferResponse approveTransfer(Long transferId, Long approvedByUserId,
                                                     Locale locale, String username) {
        String message;

        Optional<InventoryTransfer> transferOpt = repository
                .findByIdAndEntityStatusNot(transferId, EntityStatus.DELETED);

        if (transferOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryTransfer transfer = transferOpt.get();

        // Validate current status
        if (transfer.getStatus() != TransferStatus.REQUESTED) {
            message = "Transfer must be in REQUESTED status to be approved. Current status: " +
                    transfer.getStatus();
            return buildResponse(400, false, message, null, null, null);
        }

        Optional<String> approverError = procurementApproverSupport.validateApproverForOrganization(
                approvedByUserId, transfer.getProduct().getSupplierId(), locale);
        if (approverError.isPresent()) {
            return buildResponse(403, false, approverError.get(), null, null, null);
        }

        // Verify source warehouse has sufficient stock
        Optional<InventoryItem> sourceItemOpt = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        transfer.getProduct().getId(),
                        transfer.getFromLocation().getId(),
                        EntityStatus.DELETED);

        if (sourceItemOpt.isEmpty()) {
            message = "Source inventory item not found for product " + transfer.getProduct().getId() +
                    " at warehouse " + transfer.getFromLocation().getId();
            return buildResponse(400, false, message, null, null, null);
        }

        InventoryItem sourceItem = sourceItemOpt.get();
        if (sourceItem.getAvailableQuantity().compareTo(transfer.getQuantity()) < 0) {
            message = "Insufficient stock at source warehouse. Available: " +
                    sourceItem.getAvailableQuantity() + ", Required: " + transfer.getQuantity();
            return buildResponse(400, false, message, null, null, null);
        }

        // Update status to APPROVED
        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setUpdatedByUserId(approvedByUserId);
        InventoryTransfer saved = auditable.update(transfer, locale, username);

        publishTransferApprovedEvent(saved, approvedByUserId);

        InventoryTransferDto dto = mapToDto(saved);
        message = "Inventory transfer approved successfully";

        return buildResponse(200, true, message, dto, null, null);
    }

    /**
     * Rejects a REQUESTED transfer with a mandatory reason.
     */
    @Override
    @Transactional
    public InventoryTransferResponse rejectTransfer(Long transferId, Long rejectedByUserId, String rejectionReason,
                                                    Locale locale, String username) {
        String message;

        ValidatorDto validatorDto = inventoryTransferServiceValidator
                .isRejectInventoryTransferRequestValid(transferId, rejectedByUserId, rejectionReason, locale);

        if (!validatorDto.getSuccess()) {
            message = "Invalid reject inventory transfer request";
            return buildResponse(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<InventoryTransfer> transferOpt = repository
                .findByIdAndEntityStatusNot(transferId, EntityStatus.DELETED);

        if (transferOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryTransfer transfer = transferOpt.get();

        if (transfer.getStatus() != TransferStatus.REQUESTED) {
            message = "Transfer must be in REQUESTED status to be rejected. Current status: " + transfer.getStatus();
            return buildResponse(400, false, message, null, null, null);
        }

        Optional<String> approverError = procurementApproverSupport.validateApproverForOrganization(
                rejectedByUserId, transfer.getProduct().getSupplierId(), locale);
        if (approverError.isPresent()) {
            return buildResponse(403, false, approverError.get(), null, null, null);
        }

        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectionReason(rejectionReason.trim());
        transfer.setRejectedByUserId(rejectedByUserId);
        transfer.setRejectedAt(LocalDateTime.now());
        transfer.setUpdatedByUserId(rejectedByUserId);
        InventoryTransfer saved = auditable.update(transfer, locale, username);

        InventoryTransferDto dto = mapToDto(saved);
        message = "Inventory transfer rejected successfully";

        return buildResponse(200, true, message, dto, null, null);
    }

    /**
     * Moves an APPROVED transfer to IN_TRANSIT status when dispatch is complete (trip started).
     * Stock is deducted from the source warehouse at this point.
     * Only callable by the trip-tracking service after fleet allocation and trip start.
     */
    @Transactional
    public InventoryTransferResponse startTransit(Long transferId, Long startedByUserId, Long tripId, Long shipmentId,
                                                  Locale locale, String username) {
        String message;

        if (!isSystemUser(username)) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INVENTORY_TRANSFER_TRANSIT_REQUIRES_DISPATCH.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        Optional<InventoryTransfer> transferOpt = repository
                .findByIdAndEntityStatusNot(transferId, EntityStatus.DELETED);

        if (transferOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryTransfer transfer = transferOpt.get();

        if (transfer.getStatus() != TransferStatus.APPROVED) {
            message = "Transfer must be in APPROVED status to start transit. Current status: " +
                    transfer.getStatus();
            return buildResponse(400, false, message, null, null, null);
        }

        Optional<String> dispatchError = transferDispatchSupport.validateDispatchReadyForTransit(
                transferId, shipmentId, locale);
        if (dispatchError.isPresent()) {
            return buildResponse(400, false, dispatchError.get(), null, null, null);
        }

        Optional<InventoryItem> sourceItemOpt = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        transfer.getProduct().getId(),
                        transfer.getFromLocation().getId(),
                        EntityStatus.DELETED);

        if (sourceItemOpt.isEmpty()) {
            message = "Source inventory item not found";
            return buildResponse(400, false, message, null, null, null);
        }

        InventoryItem sourceItem = sourceItemOpt.get();
        Long organizationId = transfer.getProduct().getSupplierId();
        WarehouseLocation transitWarehouse = transitWarehouseSupport.resolveOrCreateTransitWarehouse(
                organizationId, locale, username);

        try {
            stockTransferSupport.transferStock(
                    transfer.getProduct(),
                    transfer.getFromLocation(),
                    transitWarehouse,
                    transfer.getQuantity(),
                    sourceItem,
                    startedByUserId,
                    transfer.getId(),
                    ReferenceDocumentType.INVENTORY_TRANSFER,
                    transfer.getUnitCost(),
                    "Transfer to in-transit holding for " + transfer.getTransferNumber(),
                    locale,
                    username);
        } catch (Exception ex) {
            log.error("Failed to move stock to transit warehouse for transfer {}: {}", transferId, ex.getMessage());
            return buildResponse(400, false, ex.getMessage(), null, null, null);
        }

        transferDispatchSupport.findShipmentForTransfer(transferId, locale)
                .ifPresent(shipment -> transfer.setShipmentId(shipment.getId()));

        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer.setUpdatedByUserId(startedByUserId);
        InventoryTransfer saved = auditable.update(transfer, locale, username);

        InventoryTransferDto dto = mapToDto(saved);
        message = "Inventory transfer started, goods are now in transit";

        return buildResponse(200, true, message, dto, null, null);
    }

    /**
     * Completes an IN_TRANSIT transfer.
     * This is when goods arrive at destination warehouse.
     * CRITICAL: This is where stock is added to the destination warehouse.
     */
    @Override
    @Transactional
    public InventoryTransferResponse completeTransfer(Long transferId, Long updatedByUserId, Locale locale,
                                                      String username, String idempotencyKey) {
        String message;

        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            boolean acquired = idempotencyService.tryAcquire(idempotencyKey,
                    IdempotencyOperation.COMPLETE_TRANSFER,
                    ReferenceDocumentType.INVENTORY_TRANSFER.name());

            if (!acquired) {
                Optional<IdempotencyKey> keyOpt = idempotencyService.findByKey(idempotencyKey);
                if (keyOpt.isPresent() && keyOpt.get().getReferenceId() != null) {
                    Long referenceId = keyOpt.get().getReferenceId();
                    Optional<InventoryTransfer> existingOpt = repository
                            .findByIdAndEntityStatusNot(referenceId, EntityStatus.DELETED);

                    if (existingOpt.isPresent()) {
                        InventoryTransferDto dto = mapToDto(existingOpt.get());
                        String msg = messageService.getMessage(
                                I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETED_SUCCESSFULLY.getCode(),
                                new String[]{}, locale);
                        return buildResponse(200, true, msg, dto, null, null);
                    }
                }
                message = messageService.getMessage(
                        I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETED_SUCCESSFULLY.getCode(),
                        new String[]{}, locale);
                return buildResponse(200, true, message, null, null, null);
            }
        }

        Optional<InventoryTransfer> transferOpt = repository
                .findByIdAndEntityStatusNot(transferId, EntityStatus.DELETED);

        if (transferOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryTransfer transfer = transferOpt.get();

        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            InventoryTransferDto dto = mapToDto(transfer);
            String msg = messageService.getMessage(
                    I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);
            return buildResponse(200, true, msg, dto, null, null);
        }

        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_IN_TRANSIT.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        if (transferDispatchSupport.hasLinkedShipment(transferId, locale)) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETE_REQUIRES_RECEIVER_ACK.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        // Verify destination inventory exists (should have been created during initial request)
        Optional<InventoryItem> destItemOpt = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        transfer.getProduct().getId(),
                        transfer.getToLocation().getId(),
                        EntityStatus.DELETED);

        if (destItemOpt.isEmpty()) {
            message = "Destination inventory not set up for product " + transfer.getProduct().getId() +
                    " at warehouse " + transfer.getToLocation().getId();
            log.error(message);
            return buildResponse(400, false, message, null, null, null);
        }

        // Receive goods at destination warehouse
        CreateOrUpdateStockRequest stockRequest = new CreateOrUpdateStockRequest();
        stockRequest.setProductId(transfer.getProduct().getId());
        stockRequest.setWarehouseLocationId(transfer.getToLocation().getId());
        stockRequest.setQuantityReceived(transfer.getQuantity());
        stockRequest.setReferenceDocumentId(transfer.getId());
        stockRequest.setReferenceDocumentType(ReferenceDocumentType.INVENTORY_TRANSFER);
        stockRequest.setUpdatedByUserId(updatedByUserId);
        stockRequest.setUnitCost(transfer.getUnitCost());
        stockRequest.setReason("Transfer in from " + transfer.getFromLocation().getName());

        try {
            inventoryItemService.createOrUpdateStock(stockRequest, locale, username);
        } catch (Exception e) {
            log.error("Failed to complete transfer at destination: {}", e.getMessage(), e);
            return buildResponse(400, false, e.getMessage(), null, null, null);
        }

        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setUpdatedByUserId(updatedByUserId);
        InventoryTransfer saved = auditable.update(transfer, locale, username);

        sendInventoryTransferCompletedInternal(saved);

        InventoryTransferDto dto = mapToDto(saved);
        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.markProcessed(idempotencyKey, saved.getId());
        }

        return buildResponse(200, true, message, dto, null, null);
    }

    /**
     * Cancels a transfer at any stage before completion.
     * Reverses any stock movements if transfer was IN_TRANSIT.
     */
    @Override
    @Transactional
    public InventoryTransferResponse cancel(Long id, Locale locale, String username) {
        String message;

        ValidatorDto validatorDto = inventoryTransferServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<InventoryTransfer> existingOpt = repository.findById(id);

        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryTransfer toDelete = existingOpt.get();
        TransferStatus previousStatus = toDelete.getStatus();

        // Cannot cancel already completed transfers
        if (previousStatus == TransferStatus.COMPLETED) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_EDITABLE.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        toDelete.setEntityStatus(EntityStatus.DELETED);
        toDelete.setStatus(TransferStatus.CANCELLED);
        InventoryTransfer saved = auditable.delete(toDelete, locale);

        if (previousStatus == TransferStatus.IN_TRANSIT) {
            Optional<InventoryItem> templateOpt = inventoryItemRepository
                    .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                            saved.getProduct().getId(),
                            saved.getToLocation().getId(),
                            EntityStatus.DELETED);
            if (templateOpt.isPresent()) {
                try {
                    WarehouseLocation transitWarehouse = transitWarehouseSupport.resolveOrCreateTransitWarehouse(
                            saved.getProduct().getSupplierId(), locale, username);
                    stockTransferSupport.transferStock(
                            saved.getProduct(),
                            transitWarehouse,
                            saved.getFromLocation(),
                            saved.getQuantity(),
                            templateOpt.get(),
                            saved.getUpdatedByUserId(),
                            saved.getId(),
                            ReferenceDocumentType.INVENTORY_TRANSFER,
                            saved.getUnitCost(),
                            "Reversal of cancelled transfer " + saved.getTransferNumber(),
                            locale,
                            username);
                } catch (Exception ex) {
                    log.error("Failed to reverse in-transit stock for cancelled transfer {}: {}",
                            saved.getTransferNumber(), ex.getMessage());
                }
            }
        }

        InventoryTransferDto dto = mapToDto(saved);
        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_DELETED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);
        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public InventoryTransferResponse findById(Long id, Locale locale, String username) {
        String message;

        ValidatorDto validatorDto = inventoryTransferServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildResponse(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<InventoryTransfer> transferOpt = repository.findByIdAndEntityStatusNotWithDetails(id, EntityStatus.DELETED);

        if (transferOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryTransferDto dto = mapToDto(transferOpt.get());
        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    @Override
    public InventoryTransferResponse findAllAsList(Locale locale, String username) {
        String message;

        List<InventoryTransfer> list = repository.findAllActiveWithDetails(EntityStatus.DELETED);

        if (list.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Map<Long, String> requesterNames = resolveRequesterNames(list);
        List<InventoryTransferDto> dtoList = list.stream()
                .map(it -> mapToDto(it, requesterNames))
                .collect(Collectors.toList());

        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, null, dtoList, null);
    }

    /**
     * Updates a transfer's basic information.
     * Can also be used to change status if status field is provided in request.
     * Status changes will trigger appropriate workflow transitions.
     */
    @Override
    @Transactional
    public InventoryTransferResponse update(EditInventoryTransferRequest request, String username, Locale locale) {
        String message;

        ValidatorDto validatorDto = inventoryTransferServiceValidator.isRequestValidForEditing(request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_UPDATE_INVALID.getCode(),
                    new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<InventoryTransfer> existingOpt = repository.findById(request.getInventoryTransferId());

        if (existingOpt.isEmpty() || existingOpt.get().getEntityStatus() == EntityStatus.DELETED) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryTransfer toEdit = existingOpt.get();

        // Cannot edit completed or cancelled transfers
        if (toEdit.getStatus() == TransferStatus.COMPLETED || toEdit.getStatus() == TransferStatus.CANCELLED) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_EDITABLE.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        // If status is being changed, validate the transition
        if (request.getStatus() != null && request.getStatus() != toEdit.getStatus()) {
            if (request.getStatus() == TransferStatus.IN_TRANSIT || request.getStatus() == TransferStatus.COMPLETED) {
                message = messageService.getMessage(
                        I18Code.MESSAGE_INVENTORY_TRANSFER_STATUS_CHANGE_NOT_ALLOWED.getCode(),
                        new String[]{}, locale);
                return buildResponse(400, false, message, null, null, null);
            }
            if (!isValidStatusTransition(toEdit.getStatus(), request.getStatus())) {
                message = "Invalid status transition from " + toEdit.getStatus() + " to " + request.getStatus();
                return buildResponse(400, false, message, null, null, null);
            }
        }

        modelMapper.map(request, toEdit);
        toEdit.setUpdatedByUserId(request.getUpdatedByUserId());
        InventoryTransfer saved = auditable.update(toEdit, locale, username);

        // Update route stops when from/to are known (including clearing en-route depots).
        if (request.getRouteStops() != null && request.getFromLocationId() != null && request.getToLocationId() != null) {
            persistTransferRouteStops(saved, request.getRouteStops(), locale, username);
        }

        InventoryTransferDto dto = mapToDto(saved);
        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    /**
     * Validates if a status transition is allowed
     */
    private boolean isValidStatusTransition(TransferStatus currentStatus, TransferStatus newStatus) {
        switch (currentStatus) {
            case REQUESTED:
                return newStatus == TransferStatus.APPROVED || newStatus == TransferStatus.CANCELLED;
            case APPROVED:
                return newStatus == TransferStatus.CANCELLED;
            case IN_TRANSIT:
                return newStatus == TransferStatus.CANCELLED;
            case COMPLETED:
            case CANCELLED:
                return false; // Terminal states
            default:
                return false;
        }
    }

    @Override
    public InventoryTransferResponse findByMultipleFilters(InventoryTransferMultipleFiltersRequest request, String username,
                                                           Locale locale) {
        String message = "";

        Specification<InventoryTransfer> spec = InventoryTransferSpecification.deleted();

        ValidatorDto validatorDto = inventoryTransferServiceValidator.isRequestValidToRetrieveInventoryTransferByMultipleFilters(
                request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (request.getProductId() != null) {
            spec = spec.and(InventoryTransferSpecification.productIdEquals(request.getProductId()));
        }
        if (request.getFromLocationId() != null) {
            spec = spec.and(InventoryTransferSpecification.fromLocationIdEquals(request.getFromLocationId()));
        }
        if (request.getToLocationId() != null) {
            spec = spec.and(InventoryTransferSpecification.toLocationIdEquals(request.getToLocationId()));
        }
        if (request.getStatus() != null) {
            spec = spec.and(InventoryTransferSpecification.statusEquals(request.getStatus()));
        }
        if (request.getSearchValue() != null && !request.getSearchValue().isBlank()) {
            spec = spec.and(InventoryTransferSpecification.any(request.getSearchValue()));
        }

        long totalCount = repository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / request.getSize());

        if (request.getPage() >= maxPage && totalCount > 0) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_PAGE_OUT_OF_BOUNDS.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        Page<InventoryTransfer> result = repository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildResponse(404, false, message, null, null, null);
        }

        Map<Long, String> requesterNames = resolveRequesterNames(result.getContent());
        Page<InventoryTransferDto> dtoPage = result.map(it -> mapToDto(it, requesterNames));

        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        InventoryTransferResponse response = buildResponse(200, true, message, null, null,
                null);
        response.setInventoryTransferDtoPage(dtoPage);
        return response;
    }

    private String generateTransferNumber() {
        LocalDateTime now = LocalDateTime.now();
        return "IT-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<InventoryTransferDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (InventoryTransferDto item : items) {
            sb.append(item.getId()).append(",")
                    .append(item.getTransferNumber()).append(",")
                    .append(item.getProductId()).append(",")
                    .append(item.getFromLocationId()).append(",")
                    .append(item.getToLocationId()).append(",")
                    .append(item.getQuantity()).append(",")
                    .append(item.getStatus().name()).append(",")
                    .append(safe(item.getReference())).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<InventoryTransferDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventory Transfers");
        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (InventoryTransferDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
            row.createCell(1).setCellValue(safe(item.getTransferNumber()));
            row.createCell(2).setCellValue(item.getProductId() != null ? item.getProductId() : 0);
            row.createCell(3).setCellValue(item.getFromLocationId() != null ? item.getFromLocationId() : 0);
            row.createCell(4).setCellValue(item.getToLocationId() != null ? item.getToLocationId() : 0);
            row.createCell(5).setCellValue(item.getQuantity() != null ? item.getQuantity().doubleValue() : 0);
            row.createCell(6).setCellValue(item.getStatus() != null ? item.getStatus().name() : "");
            row.createCell(7).setCellValue(safe(item.getReference()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<InventoryTransferDto> items) throws DocumentException {
        items = InventoryExportSupport.nullSafe(items);
        List<String[]> rows = new ArrayList<>();
        for (InventoryTransferDto item : items) {
            rows.add(new String[]{
                    String.valueOf(item.getId() != null ? item.getId() : 0),
                    safe(item.getTransferNumber()),
                    String.valueOf(item.getProductId() != null ? item.getProductId() : 0),
                    String.valueOf(item.getFromLocationId() != null ? item.getFromLocationId() : 0),
                    String.valueOf(item.getToLocationId() != null ? item.getToLocationId() : 0),
                    String.valueOf(item.getQuantity() != null ? item.getQuantity() : 0),
                    item.getStatus() != null ? item.getStatus().name() : "",
                    safe(item.getReference())
            });
        }
        return InventoryExportSupport.writeTabularPdf("Inventory Transfers", "INV-TRF",
                "Inventory transfer export", HEADERS, rows, false);
    }

    @Override
    public ImportSummary importInventoryTransferFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;
        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            List<CSVRecord> records = csvParser.getRecords();
            total = records.size();
            for (CSVRecord record : records) {
                try {
                    CreateInventoryTransferRequest request = new CreateInventoryTransferRequest();
                    request.setTransferNumber(generateTransferNumber());
                    request.setProductId(record.isMapped("PRODUCT_ID") && !record.get("PRODUCT_ID").isBlank() ? Long.parseLong(record.get("PRODUCT_ID").trim()) : null);
                    request.setFromLocationId(record.isMapped("FROM_LOCATION_ID") && !record.get("FROM_LOCATION_ID").isBlank() ? Long.parseLong(record.get("FROM_LOCATION_ID").trim()) : null);
                    request.setToLocationId(record.isMapped("TO_LOCATION_ID") && !record.get("TO_LOCATION_ID").isBlank() ? Long.parseLong(record.get("TO_LOCATION_ID").trim()) : null);
                    request.setQuantity(record.isMapped("QUANTITY") && !record.get("QUANTITY").isBlank() ? new BigDecimal(record.get("QUANTITY").trim()) : null);
                    request.setReference(record.isMapped("REFERENCE") ? record.get("REFERENCE") : null);
                    if (record.isMapped("CREATED_BY_USER_ID") && !record.get("CREATED_BY_USER_ID").isBlank()) {
                        request.setCreatedByUserId(Long.parseLong(record.get("CREATED_BY_USER_ID").trim()));
                    }
                    InventoryTransferResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");
                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + record.getRecordNumber() + ": " + response.getMessage());
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + record.getRecordNumber() + ": Unexpected error - " + e.getMessage());
                }
            }
        }
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = isSuccess ? "Import completed successfully. " + success + " out of " + total + " inventory transfers imported." : "Import failed. No inventory transfers were imported.";
        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private InventoryTransferDto mapToDto(InventoryTransfer transfer) {
        return mapToDto(transfer, Map.of());
    }

    private InventoryTransferDto mapToDto(InventoryTransfer transfer, Map<Long, String> requesterNames) {
        InventoryTransferDto dto = new InventoryTransferDto();
        dto.setId(transfer.getId());
        dto.setTransferNumber(transfer.getTransferNumber());
        dto.setQuantity(transfer.getQuantity());
        dto.setUnitCost(transfer.getUnitCost());
        dto.setStatus(transfer.getStatus());
        dto.setReference(transfer.getReference());
        dto.setCrossBorder(transfer.isCrossBorder());
        dto.setShipmentId(transfer.getShipmentId());
        dto.setRejectionReason(transfer.getRejectionReason());
        dto.setRejectedByUserId(transfer.getRejectedByUserId());
        dto.setRejectedAt(transfer.getRejectedAt());
        dto.setCreatedByUserId(transfer.getCreatedByUserId());
        dto.setUpdatedByUserId(transfer.getUpdatedByUserId());
        dto.setCreatedAt(transfer.getCreatedAt());
        dto.setUpdatedAt(transfer.getUpdatedAt());
        dto.setEntityStatus(transfer.getEntityStatus());

        Product product = transfer.getProduct();
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setProductCode(product.getProductCode());
            if (product.getUnitOfMeasure() != null) {
                dto.setUnitOfMeasure(product.getUnitOfMeasure().name());
            }
        }

        WarehouseLocation fromLocation = transfer.getFromLocation();
        if (fromLocation != null) {
            dto.setFromLocationId(fromLocation.getId());
            dto.setFromWarehouseName(fromLocation.getName());
        }

        WarehouseLocation toLocation = transfer.getToLocation();
        if (toLocation != null) {
            dto.setToLocationId(toLocation.getId());
            dto.setToWarehouseName(toLocation.getName());
        }

        Long createdByUserId = transfer.getCreatedByUserId();
        if (createdByUserId != null) {
            String cached = requesterNames.get(createdByUserId);
            if (cached != null) {
                dto.setRequestedBy(cached);
            } else {
                dto.setRequestedBy(resolveUserDisplayName(createdByUserId));
            }
        }

        // ============================================================
        // ROUTE STOPS — load logistics stops for this transfer context
        // ============================================================
        try {
            LogisticsRouteStopResponse stopsResponse = logisticsRouteStopService.findByContext(
                    RouteStopContextType.INVENTORY_TRANSFER, transfer.getId(), Locale.ENGLISH, "system");
            if (stopsResponse != null && stopsResponse.getLogisticsRouteStopDtoList() != null) {
                dto.setRouteStops(stopsResponse.getLogisticsRouteStopDtoList());
            }
        } catch (Exception ex) {
            log.warn("Failed to load route stops for transfer id={}: {}", transfer.getId(), ex.getMessage());
        }

        return dto;
    }

    private void persistTransferRouteStops(
            InventoryTransfer transfer,
            List<RouteStopRequest> routeStops,
            Locale locale,
            String username) {

        if (routeStops == null || routeStops.isEmpty()) {
            return;
        }

        Long organizationId = resolveTransferOrganizationId(transfer);
        if (organizationId == null || organizationId <= 0) {
            log.warn("Skipping route stops for transfer [id={}]: organization id could not be resolved",
                    transfer.getId());
            return;
        }

        try {
            LogisticsRouteStopResponse response = logisticsRouteStopService.replaceRouteStops(
                    RouteStopContextType.INVENTORY_TRANSFER,
                    transfer.getId(),
                    routeStops,
                    organizationId,
                    locale,
                    username);
            if (response != null && response.isSuccess()) {
                log.info("Route stops saved for transfer [id={}]", transfer.getId());
            } else {
                String detail = response != null ? response.getMessage() : "no response";
                log.warn("Failed to save route stops for transfer [id={}]: {}", transfer.getId(), detail);
            }
        } catch (Exception ex) {
            log.warn("Failed to save route stops for transfer [id={}]: {}", transfer.getId(), ex.getMessage());
        }
    }

    /**
     * Resolves the owning organization for logistics route stops.
     * Product supplier id is preferred; warehouse supplier ids are used as fallback.
     */
    private Long resolveTransferOrganizationId(InventoryTransfer transfer) {
        if (transfer == null) {
            return null;
        }

        Product product = transfer.getProduct();
        if (product != null && product.getSupplierId() != null && product.getSupplierId() > 0) {
            return product.getSupplierId();
        }

        WarehouseLocation fromLocation = transfer.getFromLocation();
        if (fromLocation != null && fromLocation.getSupplierId() != null && fromLocation.getSupplierId() > 0) {
            return fromLocation.getSupplierId();
        }

        WarehouseLocation toLocation = transfer.getToLocation();
        if (toLocation != null && toLocation.getSupplierId() != null && toLocation.getSupplierId() > 0) {
            return toLocation.getSupplierId();
        }

        return null;
    }

    private void appendRouteStopSummary(Map<String, Object> payload, Long transferId) {
        try {
            LogisticsRouteStopResponse stopsResponse = logisticsRouteStopService.findByContext(
                    RouteStopContextType.INVENTORY_TRANSFER, transferId, Locale.ENGLISH, "system");
            if (stopsResponse == null || stopsResponse.getLogisticsRouteStopDtoList() == null) {
                return;
            }
            List<Long> enRouteDepotLocationIds = stopsResponse.getLogisticsRouteStopDtoList().stream()
                    .filter(stop -> stop.getStopType() == RouteStopType.EN_ROUTE_DEPOT)
                    .map(stop -> stop.getWarehouseLocationId())
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toList());
            payload.put("enRouteDepotCount", enRouteDepotLocationIds.size());
            payload.put("enRouteDepotLocationIds", enRouteDepotLocationIds);
        } catch (Exception ex) {
            log.warn("Failed to attach route stop summary for transfer id={}: {}", transferId, ex.getMessage());
        }
    }

    private Map<Long, String> resolveRequesterNames(List<InventoryTransfer> transfers) {
        Map<Long, String> names = new java.util.HashMap<>();
        transfers.stream()
                .map(InventoryTransfer::getCreatedByUserId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .forEach(userId -> names.put(userId, resolveUserDisplayName(userId)));
        return names;
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        try {
            UserResponse response = userManagementServiceClient.findById(userId, Locale.ENGLISH);
            if (response != null && response.getUserDto() != null) {
                UserDto user = response.getUserDto();
                String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
                String last = user.getLastName() != null ? user.getLastName().trim() : "";
                String fullName = (first + " " + last).trim();
                if (!fullName.isEmpty()) {
                    return fullName;
                }
                if (user.getUsername() != null && !user.getUsername().isBlank()) {
                    return user.getUsername().trim();
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve display name for user {}: {}", userId, e.getMessage());
        }
        return "User #" + userId;
    }

    private InventoryTransferResponse buildResponse(int statusCode, boolean isSuccess, String message, InventoryTransferDto dto,
                                                    List<InventoryTransferDto> dtoList, List<String> errorMessages) {
        InventoryTransferResponse response = new InventoryTransferResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setInventoryTransferDto(dto);
        response.setInventoryTransferDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }

    private InventoryTransferResponse buildResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                              InventoryTransferDto dto, List<InventoryTransferDto> dtoList,
                                                              List<String> errorMessages) {
        return buildResponse(statusCode, isSuccess, message, dto, dtoList, errorMessages);
    }

    /**
     * Completes an IN_TRANSIT transfer and auto-creates a GRV at the destination warehouse.
     *
     * SHIPMENT/TRIP FLOW:
     * 1. Idempotency check (COMPLETE_TRANSFER_WITH_GRV)
     * 2. Load transfer — must be IN_TRANSIT
     * 3. Create GRV (PENDING) linked to transfer (no PO)
     * 4. Receive stock at destination warehouse (createOrUpdateStock)
     * 5. Mark GRV COMPLETED
     * 6. Mark transfer COMPLETED
     * 7. Publish grv.created event (with inventoryTransferId, no purchaseOrderId)
     * 8. Mark idempotency key processed
     */
    @Override
    @Transactional
    public InventoryTransferResponse completeTransferWithGrv(Long transferId, Long receivedByUserId,
                                                             String idempotencyKey, Locale locale, String username) {
        String message;

        // ============================================================
        // STEP 1: Idempotency check
        // ============================================================
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            boolean acquired = idempotencyService.tryAcquire(idempotencyKey,
                    IdempotencyOperation.COMPLETE_TRANSFER_WITH_GRV,
                    ReferenceDocumentType.INVENTORY_TRANSFER.name());

            if (!acquired) {
                Optional<IdempotencyKey> keyOpt = idempotencyService.findByKey(idempotencyKey);
                if (keyOpt.isPresent() && keyOpt.get().getReferenceId() != null) {
                    Long referenceId = keyOpt.get().getReferenceId();
                    Optional<InventoryTransfer> existingOpt = repository
                            .findByIdAndEntityStatusNot(referenceId, EntityStatus.DELETED);
                    if (existingOpt.isPresent()) {
                        InventoryTransferDto dto = mapToDto(existingOpt.get());
                        message = messageService.getMessage(
                                I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETED_SUCCESSFULLY.getCode(),
                                new String[]{}, locale);
                        return buildResponse(200, true, message, dto, null, null);
                    }
                }
                message = messageService.getMessage(
                        I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETED_SUCCESSFULLY.getCode(),
                        new String[]{}, locale);
                return buildResponse(200, true, message, null, null, null);
            }
        }

        // ============================================================
        // STEP 2: Load transfer and validate IN_TRANSIT status
        // ============================================================
        Optional<InventoryTransfer> transferOpt = repository
                .findByIdAndEntityStatusNot(transferId, EntityStatus.DELETED);

        if (transferOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        InventoryTransfer transfer = transferOpt.get();

        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            InventoryTransferDto dto = mapToDto(transfer);
            message = messageService.getMessage(
                    I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);
            return buildResponse(200, true, message, dto, null, null);
        }

        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_NOT_IN_TRANSIT.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        if (receivedByUserId == null || receivedByUserId <= 0) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETE_REQUIRES_RECEIVER_ACK.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETE_REQUIRES_RECEIVER_ACK.getCode(),
                    new String[]{}, locale);
            return buildResponse(400, false, message, null, null, null);
        }

        Optional<String> ackError = transferDispatchSupport.validateReceiverAcknowledgmentReady(transferId, locale);
        if (ackError.isPresent()) {
            return buildResponse(400, false, ackError.get(), null, null, null);
        }

        // ============================================================
        // STEP 3: Create GRV (PENDING) linked to the transfer (no PO)
        // ============================================================
        GoodsReceivedVoucher grv = new GoodsReceivedVoucher();
        grv.setGrvNumber(generateGrvNumberForTransfer(transfer));
        grv.setInventoryTransfer(transfer);
        grv.setWarehouseLocation(transfer.getToLocation());
        grv.setReceivedByUserId(receivedByUserId);
        grv.setStatus(GrvStatus.PENDING);

        GoodsReceivedVoucher savedGrv = goodsReceivedVoucherServiceAuditable.create(grv, locale, username);
        log.info("Created transfer GRV {} for transfer {}", savedGrv.getGrvNumber(), transfer.getTransferNumber());

        Optional<InventoryItem> sourceTemplateOpt = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        transfer.getProduct().getId(),
                        transfer.getFromLocation().getId(),
                        EntityStatus.DELETED);
        if (sourceTemplateOpt.isEmpty()) {
            sourceTemplateOpt = inventoryItemRepository
                    .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                            transfer.getProduct().getId(),
                            transfer.getToLocation().getId(),
                            EntityStatus.DELETED);
        }
        if (sourceTemplateOpt.isEmpty()) {
            message = "Inventory template not found for transfer completion.";
            return buildResponse(400, false, message, null, null, null);
        }

        WarehouseLocation transitWarehouse = transitWarehouseSupport.resolveOrCreateTransitWarehouse(
                transfer.getProduct().getSupplierId(), locale, username);

        try {
            stockTransferSupport.transferStock(
                    transfer.getProduct(),
                    transitWarehouse,
                    transfer.getToLocation(),
                    transfer.getQuantity(),
                    sourceTemplateOpt.get(),
                    receivedByUserId,
                    savedGrv.getId(),
                    ReferenceDocumentType.GOODS_RECEIVED_VOUCHER,
                    transfer.getUnitCost(),
                    "Transfer from in-transit to destination via GRV " + savedGrv.getGrvNumber(),
                    locale,
                    username);
        } catch (Exception e) {
            log.error("Failed to move stock from transit for transfer GRV {}: {}", savedGrv.getGrvNumber(), e.getMessage(), e);
            return buildResponse(400, false, e.getMessage(), null, null, null);
        }

        // ============================================================
        // STEP 5: Mark GRV COMPLETED
        // ============================================================
        savedGrv.setStatus(GrvStatus.COMPLETED);
        goodsReceivedVoucherServiceAuditable.update(savedGrv, locale, username);
        log.info("Marked transfer GRV {} as COMPLETED", savedGrv.getGrvNumber());

        // ============================================================
        // STEP 6: Mark transfer COMPLETED
        // ============================================================
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setUpdatedByUserId(receivedByUserId);
        InventoryTransfer saved = auditable.update(transfer, locale, username);

        // ============================================================
        // STEP 7: Publish grv.created event (with inventoryTransferId, no purchaseOrderId)
        // ============================================================
        publishTransferGrvCreatedEvent(savedGrv, saved);

        // ============================================================
        // STEP 8: Mark idempotency key processed
        // ============================================================
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.markProcessed(idempotencyKey, saved.getId());
        }

        InventoryTransferDto dto = mapToDto(saved);
        message = messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_COMPLETED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildResponse(200, true, message, dto, null, null);
    }

    private boolean isSystemUser(String username) {
        return username != null && "SYSTEM".equalsIgnoreCase(username);
    }

    private String generateGrvNumberForTransfer(InventoryTransfer transfer) {
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String transferRef = transfer.getTransferNumber().length() > 6
                ? transfer.getTransferNumber().substring(transfer.getTransferNumber().length() - 6)
                : transfer.getTransferNumber();
        return String.format("GRV-%s-TRF-%s-%s", year, transferRef, timestamp);
    }

    /**
     * Publishes inventory.transfer.approved event to inventory.exchange.
     * Consumed by the shipment/trip service to create a shipment.
     */
    private void publishTransferApprovedEvent(InventoryTransfer transfer, Long approvedByUserId) {
        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("transferId", transfer.getId());
            payload.put("transferNumber", transfer.getTransferNumber());
            payload.put("approvedByUserId", approvedByUserId);

            Product product = transfer.getProduct();
            if (product != null) {
                payload.put("organizationId", product.getSupplierId());
                payload.put("productId", product.getId());
                payload.put("productName", product.getName());
                payload.put("productCode", product.getProductCode());
            }

            WarehouseLocation from = transfer.getFromLocation();
            if (from != null) {
                payload.put("fromWarehouseLocationId", from.getId());
                payload.put("fromWarehouseName", from.getName());
            }

            WarehouseLocation to = transfer.getToLocation();
            if (to != null) {
                payload.put("toWarehouseLocationId", to.getId());
                payload.put("toWarehouseName", to.getName());
            }

            payload.put("quantity", transfer.getQuantity());
            payload.put("crossBorder", transfer.isCrossBorder());
            appendRouteStopSummary(payload, transfer.getId());
            payload.put("timestamp", LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    projectlx.inventory.management.utils.config.RabbitMQConsumerConfig.INVENTORY_EXCHANGE,
                    projectlx.inventory.management.utils.config.RabbitMQConsumerConfig.TRANSFER_APPROVED_ROUTING_KEY,
                    payload);
            log.info("Published inventory.transfer.approved event for transfer {}", transfer.getTransferNumber());
        } catch (Exception e) {
            log.error("Failed to publish inventory.transfer.approved event for transfer {}: {}",
                    transfer.getTransferNumber(), e.getMessage());
        }
    }

    /**
     * Publishes grv.created event for a transfer-backed GRV (no purchaseOrderId).
     * Downstream billing service uses inventoryTransferId instead of purchaseOrderId.
     */
    private void publishTransferGrvCreatedEvent(GoodsReceivedVoucher grv, InventoryTransfer transfer) {
        try {
            Map<String, Object> eventData = new java.util.HashMap<>();
            eventData.put("grvId", grv.getId());
            eventData.put("grvNumber", grv.getGrvNumber());
            eventData.put("grvStatus", grv.getStatus().name());
            eventData.put("inventoryTransferId", transfer.getId());
            eventData.put("warehouseLocationId", grv.getWarehouseLocation().getId());
            eventData.put("receivedByUserId", grv.getReceivedByUserId());
            eventData.put("receivedDate", grv.getReceivedDate() != null
                    ? grv.getReceivedDate().toString() : LocalDateTime.now().toString());
            eventData.put("organizationId", transfer.getFromLocation() != null
                    ? transfer.getFromLocation().getSupplierId() : null);
            eventData.put("timestamp", LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    projectlx.inventory.management.utils.config.RabbitMQConsumerConfig.INVENTORY_EXCHANGE,
                    projectlx.inventory.management.utils.config.RabbitMQConsumerConfig.GRV_CREATED_ROUTING_KEY,
                    eventData);
            log.info("Published grv.created event for transfer GRV {} to RabbitMQ", grv.getGrvNumber());
        } catch (Exception e) {
            log.error("Failed to publish grv.created event for transfer GRV {}: {}",
                    grv.getGrvNumber(), e.getMessage(), e);
        }
    }

    private void sendInventoryTransferCreatedInternal(InventoryTransfer transfer) {
        try {
            Map<String, Object> internalData = Map.of(
                    "transferNumber", transfer.getTransferNumber(),
                    "productId", transfer.getProduct() != null ? transfer.getProduct().getId() : null,
                    "fromLocationId", transfer.getFromLocation() != null ? transfer.getFromLocation().getId() : null,
                    "toLocationId", transfer.getToLocation() != null ? transfer.getToLocation().getId() : null,
                    "quantity", transfer.getQuantity(),
                    "status", transfer.getStatus() != null ? transfer.getStatus().name() : null,
                    "reference", transfer.getReference()
            );

            NotificationRequest.Recipient internalRecipient = new NotificationRequest.Recipient(
                    null, null, null, null
            );

            NotificationRequest notification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "INVENTORY_TRANSFER_CREATED_INTERNAL_EMAIL",
                    internalRecipient,
                    internalData,
                    null
            );

            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notification);
            log.info("Sent inventory transfer created internal notification for transfer {}", transfer.getTransferNumber());
        } catch (Exception e) {
            log.error("Failed to send inventory transfer created internal notification for transfer {}. Error: {}",
                    transfer.getTransferNumber(), e.getMessage());
        }
    }

    private void sendInventoryTransferCompletedInternal(InventoryTransfer transfer) {
        try {
            Map<String, Object> internalData = Map.of(
                    "transferNumber", transfer.getTransferNumber(),
                    "productId", transfer.getProduct() != null ? transfer.getProduct().getId() : null,
                    "fromLocationId", transfer.getFromLocation() != null ? transfer.getFromLocation().getId() : null,
                    "toLocationId", transfer.getToLocation() != null ? transfer.getToLocation().getId() : null,
                    "quantity", transfer.getQuantity(),
                    "status", transfer.getStatus() != null ? transfer.getStatus().name() : null,
                    "reference", transfer.getReference()
            );

            NotificationRequest.Recipient internalRecipient = new NotificationRequest.Recipient(
                    null, null, null, null
            );

            NotificationRequest notification = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "INVENTORY_TRANSFER_COMPLETED_INTERNAL_EMAIL",
                    internalRecipient,
                    internalData,
                    null
            );

            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notification);
            log.info("Sent inventory transfer completed internal notification for transfer {}", transfer.getTransferNumber());
        } catch (Exception e) {
            log.error("Failed to send inventory transfer completed internal notification for transfer {}. Error: {}",
                    transfer.getTransferNumber(), e.getMessage());
        }
    }
}