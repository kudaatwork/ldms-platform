package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.inventory.management.business.auditable.api.GoodsReceivedVoucherServiceAuditable;
import projectlx.inventory.management.business.logic.api.IdempotencyService;
import projectlx.inventory.management.business.logic.api.PurchaseOrderLineService;
import projectlx.inventory.management.business.logic.api.SalesOrderDispatchService;
import projectlx.inventory.management.business.logic.api.SalesOrderStatusManager;
import projectlx.inventory.management.business.logic.support.SalesOrderDispatchSupport;
import projectlx.inventory.management.business.logic.support.StockTransferSupport;
import projectlx.inventory.management.business.logic.support.TransitWarehouseSupport;
import projectlx.inventory.management.model.*;
import projectlx.inventory.management.repository.*;
import projectlx.inventory.management.utils.config.RabbitMQConsumerConfig;
import projectlx.inventory.management.utils.dtos.SalesOrderDto;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.responses.SalesOrderResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class SalesOrderDispatchServiceImpl implements SalesOrderDispatchService {

    private static final String SYSTEM_USERNAME = "SYSTEM";

    private final SalesOrderRepository salesOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final GoodsReceivedVoucherServiceAuditable goodsReceivedVoucherServiceAuditable;
    private final SalesOrderStatusManager salesOrderStatusManager;
    private final SalesOrderDispatchSupport salesOrderDispatchSupport;
    private final StockTransferSupport stockTransferSupport;
    private final TransitWarehouseSupport transitWarehouseSupport;
    private final PurchaseOrderLineService purchaseOrderLineService;
    private final IdempotencyService idempotencyService;
    private final RabbitTemplate rabbitTemplate;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public SalesOrderResponse startDispatch(Long salesOrderId, Long startedByUserId, Long tripId, Long shipmentId,
                                            Locale locale, String username) {
        if (!SYSTEM_USERNAME.equalsIgnoreCase(username)) {
            return error(400, messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_DISPATCH_REQUIRES_TRIP.getCode(), new String[]{}, locale));
        }

        Optional<SalesOrder> soOpt = salesOrderRepository.findByIdAndEntityStatusNot(salesOrderId, EntityStatus.DELETED);
        if (soOpt.isEmpty()) {
            return error(404, messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        SalesOrder salesOrder = soOpt.get();
        if (salesOrder.getStatus() == SalesOrderStatus.SHIPPED) {
            return success(salesOrder, messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_DISPATCH_STARTED.getCode(), new String[]{}, locale));
        }

        if (salesOrder.getStatus() != SalesOrderStatus.APPROVED) {
            return error(400, "Sales order must be APPROVED to start dispatch. Current: " + salesOrder.getStatus());
        }

        if (salesOrder.getFulfillmentWarehouseId() == null) {
            return error(400, "Fulfillment warehouse is required before dispatch.");
        }

        Optional<String> dispatchError = salesOrderDispatchSupport.validateDispatchReadyForTransit(
                salesOrderId, shipmentId, locale);
        if (dispatchError.isPresent()) {
            return error(400, dispatchError.get());
        }

        Optional<WarehouseLocation> fulfillmentWarehouseOpt = warehouseLocationRepository
                .findById(salesOrder.getFulfillmentWarehouseId());
        if (fulfillmentWarehouseOpt.isEmpty()) {
            return error(400, "Fulfillment warehouse not found.");
        }
        WarehouseLocation fulfillmentWarehouse = fulfillmentWarehouseOpt.get();

        try {
            salesOrderStatusManager.transition(
                    salesOrder,
                    SalesOrderStatus.SHIPPED,
                    salesOrder.getFulfillmentWarehouseId(),
                    username,
                    locale);
        } catch (IllegalStateException ex) {
            return error(400, ex.getMessage());
        }

        WarehouseLocation transitWarehouse = transitWarehouseSupport.resolveOrCreateTransitWarehouse(
                salesOrder.getSupplierOrganizationId(), locale, username);

        for (SalesOrderLine line : salesOrder.getSalesOrderLines()) {
            BigDecimal qty = line.getQuantity();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Optional<InventoryItem> sourceTemplateOpt = inventoryItemRepository
                    .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                            line.getProduct().getId(), fulfillmentWarehouse.getId(), EntityStatus.DELETED);
            if (sourceTemplateOpt.isEmpty()) {
                return error(400, "Inventory not found for product " + line.getProduct().getName()
                        + " at fulfillment warehouse.");
            }

            try {
                stockTransferSupport.transferStock(
                        line.getProduct(),
                        fulfillmentWarehouse,
                        transitWarehouse,
                        qty,
                        sourceTemplateOpt.get(),
                        startedByUserId,
                        salesOrder.getId(),
                        ReferenceDocumentType.SALES_ORDER,
                        line.getUnitPrice(),
                        "Customer order dispatch " + salesOrder.getSalesOrderNumber(),
                        locale,
                        username);
            } catch (Exception ex) {
                log.error("Failed to move SO {} line {} to transit: {}", salesOrderId, line.getId(), ex.getMessage());
                return error(400, ex.getMessage());
            }

            consumeReservations(salesOrder, line, fulfillmentWarehouse.getId(), qty);
            line.setFulfilledQuantity(qty);
        }

        salesOrderDispatchSupport.findShipmentForSalesOrder(salesOrderId, locale)
                .ifPresent(shipment -> {
                    salesOrder.setShipmentId(shipment.getId());
                    salesOrder.setShipmentCreatedAt(LocalDateTime.now());
                });

        SalesOrder saved = salesOrderRepository.save(salesOrder);
        log.info("Sales order {} dispatch started (trip={}, shipment={})", salesOrderId, tripId, shipmentId);
        return success(saved, messageService.getMessage(
                I18Code.MESSAGE_SALES_ORDER_DISPATCH_STARTED.getCode(), new String[]{}, locale));
    }

    @Override
    @Transactional
    public SalesOrderResponse completeWithGrv(Long salesOrderId, Long receivedByUserId, String idempotencyKey,
                                              Locale locale, String username) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            boolean acquired = idempotencyService.tryAcquire(idempotencyKey,
                    IdempotencyOperation.COMPLETE_SALES_ORDER_WITH_GRV,
                    ReferenceDocumentType.SALES_ORDER.name());
            if (!acquired) {
                Optional<IdempotencyKey> keyOpt = idempotencyService.findByKey(idempotencyKey);
                if (keyOpt.isPresent() && keyOpt.get().getReferenceId() != null) {
                    return salesOrderRepository.findByIdAndEntityStatusNot(keyOpt.get().getReferenceId(), EntityStatus.DELETED)
                            .map(so -> success(so, messageService.getMessage(
                                    I18Code.MESSAGE_SALES_ORDER_DELIVERY_COMPLETED.getCode(), new String[]{}, locale)))
                            .orElse(success(null, messageService.getMessage(
                                    I18Code.MESSAGE_SALES_ORDER_DELIVERY_COMPLETED.getCode(), new String[]{}, locale)));
                }
            }
        }

        Optional<SalesOrder> soOpt = salesOrderRepository.findByIdAndEntityStatusNot(salesOrderId, EntityStatus.DELETED);
        if (soOpt.isEmpty()) {
            return error(404, messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }

        SalesOrder salesOrder = soOpt.get();
        if (salesOrder.getStatus() == SalesOrderStatus.DELIVERED
                || salesOrder.getStatus() == SalesOrderStatus.FULFILLED) {
            return success(salesOrder, messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_DELIVERY_COMPLETED.getCode(), new String[]{}, locale));
        }

        if (salesOrder.getStatus() != SalesOrderStatus.SHIPPED) {
            return error(400, "Sales order must be SHIPPED to complete delivery. Current: " + salesOrder.getStatus());
        }

        if (receivedByUserId == null || receivedByUserId <= 0
                || idempotencyKey == null || idempotencyKey.isBlank()) {
            return error(400, messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_COMPLETE_REQUIRES_RECEIVER_ACK.getCode(), new String[]{}, locale));
        }

        Optional<String> ackError = salesOrderDispatchSupport.validateReceiverAcknowledgmentReady(salesOrderId, locale);
        if (ackError.isPresent()) {
            return error(400, ackError.get());
        }

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository
                .findByIdAndEntityStatusNot(salesOrder.getPurchaseOrderId(), EntityStatus.DELETED);
        if (poOpt.isEmpty()) {
            return error(404, "Linked purchase order not found.");
        }
        PurchaseOrder purchaseOrder = poOpt.get();

        Optional<WarehouseLocation> receivingWarehouseOpt = warehouseLocationRepository
                .findById(purchaseOrder.getReceivingWarehouseId());
        if (receivingWarehouseOpt.isEmpty()) {
            return error(400, "Customer receiving warehouse not found.");
        }
        WarehouseLocation receivingWarehouse = receivingWarehouseOpt.get();

        WarehouseLocation transitWarehouse = transitWarehouseSupport.resolveOrCreateTransitWarehouse(
                salesOrder.getSupplierOrganizationId(), locale, username);

        GoodsReceivedVoucher grv = new GoodsReceivedVoucher();
        grv.setGrvNumber(generateGrvNumber(purchaseOrder));
        grv.setPurchaseOrder(purchaseOrder);
        grv.setWarehouseLocation(receivingWarehouse);
        grv.setReceivedByUserId(receivedByUserId);
        grv.setStatus(GrvStatus.PENDING);
        GoodsReceivedVoucher savedGrv = goodsReceivedVoucherServiceAuditable.create(grv, locale, username);

        boolean allLinesReceived = true;
        for (SalesOrderLine soLine : salesOrder.getSalesOrderLines()) {
            BigDecimal qty = soLine.getQuantity();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Optional<PurchaseOrderLine> poLineOpt = purchaseOrder.getPurchaseOrderLines().stream()
                    .filter(line -> line.getProduct().getId().equals(soLine.getProduct().getId()))
                    .findFirst();
            if (poLineOpt.isEmpty()) {
                return error(400, "Purchase order line not found for product " + soLine.getProduct().getId());
            }

            PurchaseOrderLine poLine = poLineOpt.get();
            PurchaseOrderLine updatedLine = purchaseOrderLineService.updateReceivedQuantity(
                    poLine.getId(), qty, receivedByUserId, locale, username);

            Optional<InventoryItem> templateOpt = inventoryItemRepository
                    .findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                            soLine.getProduct().getId(), transitWarehouse.getId(), EntityStatus.DELETED);
            if (templateOpt.isEmpty()) {
                templateOpt = inventoryItemRepository.findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        soLine.getProduct().getId(), salesOrder.getFulfillmentWarehouseId(), EntityStatus.DELETED);
            }
            if (templateOpt.isEmpty()) {
                return error(400, "Inventory template not found for delivery completion.");
            }

            try {
                stockTransferSupport.transferStock(
                        soLine.getProduct(),
                        transitWarehouse,
                        receivingWarehouse,
                        qty,
                        templateOpt.get(),
                        receivedByUserId,
                        savedGrv.getId(),
                        ReferenceDocumentType.GOODS_RECEIVED_VOUCHER,
                        soLine.getUnitPrice(),
                        "Customer delivery via GRV " + savedGrv.getGrvNumber(),
                        locale,
                        username);
            } catch (Exception ex) {
                log.error("Failed to receive SO {} at customer warehouse: {}", salesOrderId, ex.getMessage());
                return error(400, ex.getMessage());
            }

            if (updatedLine.getReceivedQuantity().compareTo(updatedLine.getQuantity()) < 0) {
                allLinesReceived = false;
            }
        }

        if (allLinesReceived) {
            purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
            purchaseOrder.setReceivedDate(LocalDateTime.now());
        } else {
            purchaseOrder.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }
        purchaseOrder.setUpdatedByUserId(receivedByUserId);
        purchaseOrderRepository.save(purchaseOrder);

        savedGrv.setStatus(GrvStatus.COMPLETED);
        savedGrv.setReceivedDate(LocalDateTime.now());
        goodsReceivedVoucherServiceAuditable.update(savedGrv, locale, username);

        publishSalesOrderGrvCreatedEvent(savedGrv, purchaseOrder, salesOrder, allLinesReceived);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.markProcessed(idempotencyKey, salesOrder.getId());
        }

        SalesOrder refreshed = salesOrderRepository.findByIdAndEntityStatusNot(salesOrderId, EntityStatus.DELETED)
                .orElse(salesOrder);
        return success(refreshed, messageService.getMessage(
                I18Code.MESSAGE_SALES_ORDER_DELIVERY_COMPLETED.getCode(), new String[]{}, locale));
    }

    private void consumeReservations(SalesOrder salesOrder, SalesOrderLine line, Long warehouseId, BigDecimal qty) {
        List<InventoryReservation> reservations = inventoryReservationRepository
                .findBySalesOrderIdAndSalesOrderLineIdAndWarehouseLocation_IdAndStatusOrderByCreatedAtAsc(
                        salesOrder.getId(), line.getId(), warehouseId, ReservationStatus.ACTIVE);

        BigDecimal remaining = qty;
        for (InventoryReservation res : reservations) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal consume = res.getQuantity().min(remaining);
            BigDecimal newQty = res.getQuantity().subtract(consume);
            if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
                res.setQuantity(BigDecimal.ZERO);
                res.setStatus(ReservationStatus.CONSUMED);
            } else {
                res.setQuantity(newQty);
            }
            inventoryReservationRepository.save(res);
            remaining = remaining.subtract(consume);
        }

        inventoryItemRepository.findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                        line.getProduct().getId(), warehouseId, EntityStatus.DELETED)
                .ifPresent(item -> {
                    BigDecimal currentReserved = item.getReservedQuantity() != null
                            ? item.getReservedQuantity() : BigDecimal.ZERO;
                    item.setReservedQuantity(currentReserved.subtract(qty).max(BigDecimal.ZERO));
                    inventoryItemRepository.save(item);
                });
    }

    private void publishSalesOrderGrvCreatedEvent(GoodsReceivedVoucher grv, PurchaseOrder purchaseOrder,
                                                  SalesOrder salesOrder, boolean fullyReceived) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("grvId", grv.getId());
            eventData.put("grvNumber", grv.getGrvNumber());
            eventData.put("grvStatus", grv.getStatus().name());
            eventData.put("purchaseOrderId", purchaseOrder.getId());
            eventData.put("purchaseOrderNumber", purchaseOrder.getPurchaseOrderNumber());
            eventData.put("purchaseOrderStatus", purchaseOrder.getStatus().name());
            eventData.put("salesOrderId", salesOrder.getId());
            eventData.put("salesOrderNumber", salesOrder.getSalesOrderNumber());
            eventData.put("warehouseLocationId", grv.getWarehouseLocation().getId());
            eventData.put("receivedByUserId", grv.getReceivedByUserId());
            eventData.put("receivedDate", grv.getReceivedDate().toString());
            eventData.put("fullyReceived", fullyReceived);
            eventData.put("supplierId", purchaseOrder.getSupplierId());
            eventData.put("supplierOrganizationId", salesOrder.getSupplierOrganizationId());
            eventData.put("organizationId", purchaseOrder.getOrganizationId());
            eventData.put("timestamp", LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConsumerConfig.INVENTORY_EXCHANGE,
                    RabbitMQConsumerConfig.GRV_CREATED_ROUTING_KEY,
                    eventData);
            log.info("Published grv.created for customer delivery GRV {}", grv.getGrvNumber());
        } catch (Exception ex) {
            log.error("Failed to publish grv.created for SO delivery: {}", ex.getMessage(), ex);
        }
    }

    private String generateGrvNumber(PurchaseOrder purchaseOrder) {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String poRef = purchaseOrder.getPurchaseOrderNumber().substring(
                Math.max(0, purchaseOrder.getPurchaseOrderNumber().length() - 6));
        return String.format("GRV-%s-%s-%s", year, poRef, timestamp);
    }

    private SalesOrderResponse success(SalesOrder salesOrder, String message) {
        SalesOrderResponse response = new SalesOrderResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage(message);
        if (salesOrder != null) {
            modelMapper.getConfiguration().setAmbiguityIgnored(true);
            response.setSalesOrderDto(modelMapper.map(salesOrder, SalesOrderDto.class));
        }
        return response;
    }

    private SalesOrderResponse error(int status, String message) {
        SalesOrderResponse response = new SalesOrderResponse();
        response.setStatusCode(status);
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
