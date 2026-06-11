package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.auditable.api.GoodsReceivedVoucherServiceAuditable;
import projectlx.inventory.management.business.logic.api.*;
import projectlx.inventory.management.business.validator.api.GoodsReceivedVoucherServiceValidator;
import projectlx.inventory.management.repository.SalesOrderRepository;
import projectlx.inventory.management.exceptions.GoodsReceiptException;
import projectlx.inventory.management.model.*;
import projectlx.inventory.management.repository.GoodsReceivedVoucherRepository;
import projectlx.inventory.management.repository.PurchaseOrderRepository;
import projectlx.inventory.management.utils.dtos.GoodsReceiptResult;
import projectlx.inventory.management.utils.dtos.ProcessedReceiptItem;
import projectlx.inventory.management.utils.dtos.ReceiptValidationError;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.inventory.management.utils.requests.CreateOrUpdateStockRequest;
import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptProcessorImpl implements GoodsReceiptProcessor {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceivedVoucherRepository grvRepository;
    private final GoodsReceivedVoucherServiceAuditable goodsReceivedVoucherServiceAuditable;
    private final InventoryItemService inventoryItemService;
    private final PurchaseOrderLineService purchaseOrderLineService;
    private final IdempotencyService idempotencyService;
    private final GoodsReceivedVoucherServiceValidator grvValidator;
    private final RabbitTemplate rabbitTemplate;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderStatusManager salesOrderStatusManager;

    private static final String GRV_EXCHANGE = "inventory.exchange";
    private static final String GRV_CREATED_ROUTING_KEY = "grv.created";

    /**
     * Process goods receipt: Receive Goods action → Create GRV document
     *
     * BACK-TO-BACK FULFILLMENT FLOW:
     * 1. Validate request
     * 2. Check idempotency
     * 3. Load and validate PO
     * 4. Validate warehouse
     * 5. Pre-validate all line items
     * 6. Create GRV (status=PENDING)
     * 7. Process each line item (update PO line + inventory)
     * 8. Update PO status
     * 9. Mark GRV as COMPLETED
     * 10. UPDATE LINKED SALES ORDER: AWAITING_RECEIPT → PENDING (NEW!)
     * 11. Publish grv.created event
     * 12. Mark idempotency key
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public GoodsReceiptResult processGoodsReceipt(ReceiveGoodsRequest request,
                                                  String username, Locale locale) {

        String idempotencyKey = request.getIdempotencyKey();

        // ============================================================
        // STEP 1: Validate incoming request
        // ============================================================
        ValidatorDto validatorDto = grvValidator.isReceiveGoodsRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            return GoodsReceiptResult.failure(
                    validatorDto.getErrorMessages().stream()
                            .map(msg -> new ReceiptValidationError(null, msg))
                            .collect(java.util.stream.Collectors.toList())
            );
        }

        // ============================================================
        // STEP 2: Idempotency check
        // ============================================================
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            boolean acquired = idempotencyService.tryAcquire(idempotencyKey,
                    IdempotencyOperation.RECEIVE_GOODS,
                    ReferenceDocumentType.GOODS_RECEIVED_VOUCHER.name());

            if (!acquired) {
                try {
                    Optional<IdempotencyKey> idempotencyKeyOptional = idempotencyService.findByKey(idempotencyKey);
                    if (idempotencyKeyOptional.isPresent() && idempotencyKeyOptional.get().getReferenceId() != null) {
                        Long grvId = idempotencyKeyOptional.get().getReferenceId();
                        Optional<GoodsReceivedVoucher> existingGrvOpt = grvRepository
                                .findByIdAndEntityStatusNot(grvId, EntityStatus.DELETED);
                        if (existingGrvOpt.isPresent()) {
                            log.info("Returning existing GRV {} for idempotency key {}",
                                    grvId, idempotencyKey);
                            // Return success with empty processed items (already processed)
                            return GoodsReceiptResult.success(Collections.emptyList());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Idempotency lookup failed for key {}: {}", idempotencyKey, ex.getMessage());
                }

                log.warn("Duplicate request detected with idempotency key: {}", idempotencyKey);
                return GoodsReceiptResult.success(Collections.emptyList());
            }
        }

        // ============================================================
        // STEP 3: Load and validate Purchase Order
        // ============================================================
        Optional<PurchaseOrder> existingPurchaseOrder = purchaseOrderRepository
                .findByIdAndEntityStatusNot(request.getPurchaseOrderId(), EntityStatus.DELETED);

        if (existingPurchaseOrder.isEmpty()) {
            return GoodsReceiptResult.failure(Collections.singletonList(
                    new ReceiptValidationError(null, "Purchase order not found: " + request.getPurchaseOrderId())
            ));
        }

        PurchaseOrder purchaseOrder = existingPurchaseOrder.get();

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.APPROVED &&
                purchaseOrder.getStatus() != PurchaseOrderStatus.SUBMITTED &&
                purchaseOrder.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            return GoodsReceiptResult.failure(Collections.singletonList(
                    new ReceiptValidationError(null,
                            "Purchase order not in receivable status: " + purchaseOrder.getStatus())
            ));
        }

        // ============================================================
        // STEP 4: Validate warehouse location
        // ============================================================
        Optional<WarehouseLocation> locationOpt = inventoryItemService
                .findWarehouseLocationBySupplierId(purchaseOrder.getSupplierId(),
                        request.getWarehouseLocationId());

        if (locationOpt.isEmpty()) {
            return GoodsReceiptResult.failure(Collections.singletonList(
                    new ReceiptValidationError(null,
                            "Warehouse location not found or doesn't belong to supplier")
            ));
        }

        // ============================================================
        // STEP 5: Pre-validate ALL line items before processing any
        // ============================================================
        List<ReceiptValidationError> preValidationErrors = validateAllLineItems(
                purchaseOrder, request.getReceivedItems());

        if (!preValidationErrors.isEmpty()) {
            return GoodsReceiptResult.failure(preValidationErrors);
        }

        try {
            // ============================================================
            // STEP 6: CREATE GRV (with status PENDING)
            // ============================================================
            GoodsReceivedVoucher goodsReceivedVoucher = new GoodsReceivedVoucher();
            goodsReceivedVoucher.setGrvNumber(generateGrvNumber(purchaseOrder));
            goodsReceivedVoucher.setPurchaseOrder(purchaseOrder);
            goodsReceivedVoucher.setWarehouseLocation(locationOpt.get());
            goodsReceivedVoucher.setReceivedByUserId(request.getReceivedByUserId());
            goodsReceivedVoucher.setNotes(request.getNotes());
            goodsReceivedVoucher.setStatus(GrvStatus.PENDING);

            GoodsReceivedVoucher savedGrv = goodsReceivedVoucherServiceAuditable
                    .create(goodsReceivedVoucher, locale, username);
            log.info("Created GRV {} for PO {}", savedGrv.getGrvNumber(), purchaseOrder.getPurchaseOrderNumber());

            // ============================================================
            // STEP 7: Process each received line item
            // ============================================================
            boolean allPurchaseOrderLinesReceived = true;
            List<ProcessedReceiptItem> processedItems = new ArrayList<>();

            for (ReceiveGoodsRequest.ReceivedLineItem receivedItem : request.getReceivedItems()) {

                Optional<PurchaseOrderLine> purchaseOrderLineOpt = purchaseOrder.getPurchaseOrderLines().stream()
                        .filter(line -> line.getId().equals(receivedItem.getPurchaseOrderLineId()))
                        .findFirst();

                if (purchaseOrderLineOpt.isEmpty()) {
                    throw new IllegalArgumentException("Purchase order line " +
                            receivedItem.getPurchaseOrderLineId() + " not found");
                }

                PurchaseOrderLine orderLine = purchaseOrderLineOpt.get();

                // Update the received quantity on the PO line
                PurchaseOrderLine updatedLine = purchaseOrderLineService.updateReceivedQuantity(
                        receivedItem.getPurchaseOrderLineId(),
                        receivedItem.getQuantityReceived(),
                        request.getReceivedByUserId(),
                        locale,
                        username
                );

                log.info("Updated PO line {} received quantity to {}", updatedLine.getId(),
                        updatedLine.getReceivedQuantity());

                // Update inventory (references GRV, not PO)
                CreateOrUpdateStockRequest stockRequest = new CreateOrUpdateStockRequest();
                stockRequest.setProductId(orderLine.getProduct().getId());
                stockRequest.setWarehouseLocationId(request.getWarehouseLocationId());
                stockRequest.setQuantityReceived(receivedItem.getQuantityReceived());
                stockRequest.setReason("Received against GRV " + savedGrv.getGrvNumber());
                stockRequest.setUserId(request.getReceivedByUserId());
                stockRequest.setReferenceDocumentId(savedGrv.getId());
                stockRequest.setReferenceDocumentType(ReferenceDocumentType.GOODS_RECEIVED_VOUCHER);
                stockRequest.setUnitCost(orderLine.getUnitPrice());

                inventoryItemService.createOrUpdateStock(stockRequest, locale, username);
                log.info("Updated inventory for product {} with quantity {} against GRV {}",
                        orderLine.getProduct().getId(), receivedItem.getQuantityReceived(),
                        savedGrv.getGrvNumber());

                // Track processed item
                processedItems.add(new ProcessedReceiptItem(updatedLine, receivedItem.getQuantityReceived()));

                // Check if all quantities for this line have been received
                if (updatedLine.getReceivedQuantity().compareTo(updatedLine.getQuantity()) < 0) {
                    allPurchaseOrderLinesReceived = false;
                }
            }

            // ============================================================
            // STEP 8: Update Purchase Order status based on completion
            // ============================================================
            if (allPurchaseOrderLinesReceived) {
                purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
                purchaseOrder.setReceivedDate(LocalDateTime.now());
                log.info("PO {} fully received", purchaseOrder.getPurchaseOrderNumber());
            } else {
                purchaseOrder.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
                log.info("PO {} partially received", purchaseOrder.getPurchaseOrderNumber());
            }

            purchaseOrder.setUpdatedByUserId(request.getReceivedByUserId());
            purchaseOrderRepository.save(purchaseOrder);

            // ============================================================
            // STEP 9: Update GRV status to COMPLETED
            // ============================================================
            savedGrv.setStatus(GrvStatus.COMPLETED);
            savedGrv.setReceivedDate(LocalDateTime.now());
            goodsReceivedVoucherServiceAuditable.update(savedGrv, locale, username);
            log.info("Marked GRV {} as COMPLETED", savedGrv.getGrvNumber());

            // ============================================================
            // STEP 10: UPDATE LINKED SALES ORDER (Back-to-Back Flow)
            // Transition SO from AWAITING_RECEIPT → PENDING
            // This unlocks the SO for confirmation and stock reservation
            // ============================================================
            updateLinkedSalesOrder(purchaseOrder, request.getWarehouseLocationId(), username, locale);

            // ============================================================
            // STEP 11: Publish grv.created event to RabbitMQ
            // ============================================================
            publishGrvCreatedEvent(savedGrv, purchaseOrder, allPurchaseOrderLinesReceived);

            // ============================================================
            // STEP 12: Mark idempotency key as processed
            // ============================================================
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.markProcessed(idempotencyKey, savedGrv.getId());
                log.info("Marked idempotency key {} as processed with GRV {}",
                        idempotencyKey, savedGrv.getId());
            }

            log.info("Successfully processed goods receipt for PO {} with {} items and created GRV {}",
                    purchaseOrder.getId(), processedItems.size(), savedGrv.getGrvNumber());

            return GoodsReceiptResult.success(processedItems);

        } catch (Exception e) {
            log.error("Failed to process goods receipt for PO {}: {}",
                    request.getPurchaseOrderId(), e.getMessage(), e);
            throw new GoodsReceiptException("Goods receipt processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate all line items before processing any
     */
    private List<ReceiptValidationError> validateAllLineItems(PurchaseOrder purchaseOrder,
                                                              List<ReceiveGoodsRequest.ReceivedLineItem> receivedItems) {
        List<ReceiptValidationError> errors = new ArrayList<>();

        for (ReceiveGoodsRequest.ReceivedLineItem item : receivedItems) {
            // Validate line exists
            Optional<PurchaseOrderLine> lineOpt = purchaseOrder.getPurchaseOrderLines().stream()
                    .filter(line -> line.getId().equals(item.getPurchaseOrderLineId()))
                    .findFirst();

            if (lineOpt.isEmpty()) {
                errors.add(new ReceiptValidationError(item.getPurchaseOrderLineId(),
                        "Purchase order line not found"));
                continue;
            }

            // Validate quantity
            PurchaseOrderLine line = lineOpt.get();
            BigDecimal remainingToReceive = line.getQuantity().subtract(line.getReceivedQuantity());
            if (item.getQuantityReceived().compareTo(remainingToReceive) > 0) {
                errors.add(new ReceiptValidationError(item.getPurchaseOrderLineId(),
                        String.format("Cannot receive %s - only %s remaining for line %s",
                                item.getQuantityReceived(), remainingToReceive, line.getId())));
            }
        }

        return errors;
    }

    /**
     * Generate unique GRV number
     */
    private String generateGrvNumber(PurchaseOrder purchaseOrder) {
        String prefix = "GRV";
        String year = String.valueOf(LocalDateTime.now().getYear());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String poRef = purchaseOrder.getPurchaseOrderNumber().substring(
                Math.max(0, purchaseOrder.getPurchaseOrderNumber().length() - 6));

        return String.format("%s-%s-%s-%s", prefix, year, poRef, timestamp);
    }

    /**
     * Update linked Sales Order when goods are received (Back-to-Back Flow)
     *
     * If a Sales Order exists for this Purchase Order and is in AWAITING_RECEIPT status,
     * transition it to PENDING so it can be confirmed and stock reserved.
     *
     * @param purchaseOrder The PO that goods were received against
     * @param warehouseLocationId The warehouse where goods were received
     * @param username The user performing the action
     * @param locale User locale
     */
    private void updateLinkedSalesOrder(PurchaseOrder purchaseOrder,
                                        Long warehouseLocationId,
                                        String username,
                                        Locale locale) {
        try {
            // Find linked Sales Order for this PO
            Optional<SalesOrder> linkedSalesOrder = salesOrderRepository
                    .findByPurchaseOrderIdAndEntityStatusNot(
                            purchaseOrder.getId(),
                            EntityStatus.DELETED);

            if (linkedSalesOrder.isEmpty()) {
                log.debug("No linked Sales Order found for PO {} - skipping SO update",
                        purchaseOrder.getPurchaseOrderNumber());
                return;
            }

            SalesOrder salesOrder = linkedSalesOrder.get();

            // Only transition if SO is in AWAITING_RECEIPT status
            if (salesOrder.getStatus() != SalesOrderStatus.AWAITING_RECEIPT) {
                log.debug("Sales Order {} is not in AWAITING_RECEIPT status (current: {}) - skipping transition",
                        salesOrder.getSalesOrderNumber(), salesOrder.getStatus());
                return;
            }

            // Transition SO from AWAITING_RECEIPT → PENDING
            log.info("Transitioning Sales Order {} from AWAITING_RECEIPT to PENDING - goods received",
                    salesOrder.getSalesOrderNumber());

            salesOrderStatusManager.transition(
                    salesOrder,
                    SalesOrderStatus.PENDING,
                    warehouseLocationId,
                    username,
                    locale
            );

            log.info("Sales Order {} now PENDING and ready for confirmation",
                    salesOrder.getSalesOrderNumber());

        } catch (Exception e) {
            // Log warning but don't fail the goods receipt
            // The SO can be manually transitioned if needed
            log.warn("Failed to update linked Sales Order for PO {}: {}",
                    purchaseOrder.getPurchaseOrderNumber(), e.getMessage());
        }
    }

    /**
     * Publish grv.created event for downstream services (Phase H: Billing Service).
     * Includes inventoryTransferId when the GRV was created from a transfer (no PO).
     */
    private void publishGrvCreatedEvent(GoodsReceivedVoucher grv,
                                        PurchaseOrder purchaseOrder,
                                        boolean fullyReceived) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("grvId", grv.getId());
            eventData.put("grvNumber", grv.getGrvNumber());
            eventData.put("grvStatus", grv.getStatus().name());
            eventData.put("purchaseOrderId", purchaseOrder.getId());
            eventData.put("purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber());
            eventData.put("purchaseOrderStatus", purchaseOrder.getStatus().name());
            eventData.put("warehouseLocationId", grv.getWarehouseLocation().getId());
            eventData.put("receivedByUserId", grv.getReceivedByUserId());
            eventData.put("receivedDate", grv.getReceivedDate().toString());
            eventData.put("fullyReceived", fullyReceived);
            eventData.put("supplierId", purchaseOrder.getSupplierId());
            eventData.put("organizationId", purchaseOrder.getOrganizationId());
            if (grv.getInventoryTransfer() != null) {
                eventData.put("inventoryTransferId", grv.getInventoryTransfer().getId());
            }
            eventData.put("timestamp", LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(GRV_EXCHANGE, GRV_CREATED_ROUTING_KEY, eventData);
            log.info("Published grv.created event for GRV {} to RabbitMQ", grv.getGrvNumber());
        } catch (Exception e) {
            log.error("Failed to publish grv.created event for GRV {}: {}",
                    grv.getGrvNumber(), e.getMessage(), e);
            // Don't throw - GRV is saved, event failure shouldn't rollback transaction
        }
    }
}