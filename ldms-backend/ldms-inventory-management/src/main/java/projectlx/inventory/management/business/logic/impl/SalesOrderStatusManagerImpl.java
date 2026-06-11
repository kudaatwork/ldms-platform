package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.logic.api.InventoryAllocationService;
import projectlx.inventory.management.business.logic.api.SalesOrderStatusManager;
import projectlx.inventory.management.business.logic.impl.InventoryAllocationServiceImpl.AllocationResult;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.inventory.management.repository.SalesOrderRepository;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Sales Order Status Manager with Stock Reservation
 *
 * TWO SUPPORTED FULFILLMENT FLOWS:
 *
 * FLOW A - PAYMENT-GATED (new default):
 * PO: DRAFT → SUBMITTED → (multi-stage approval) → APPROVED
 *   ↓ (billing creates invoice, customer pays)
 * SO: PENDING_APPROVAL (created after payment verified)
 *   ↓ (internal SO approval)
 * SO: APPROVED ← STOCK RESERVED AT SHIPMENT START ✅
 *   ↓
 * SO: PARTIALLY_SHIPPED → SHIPPED → DELIVERED → FULFILLED
 *
 * FLOW B - LEGACY BACK-TO-BACK:
 * SO: AWAITING_RECEIPT → PENDING → CONFIRMED ← STOCK RESERVED HERE ✅
 *   ↓
 * SO: PARTIALLY_SHIPPED → SHIPPED → DELIVERED → FULFILLED
 *
 * KEY RULES:
 * 1. PENDING_APPROVAL → APPROVED: internal SO approval, no stock yet
 * 2. APPROVED → PARTIALLY_SHIPPED/SHIPPED: stock reserved at shipment start
 * 3. PENDING → CONFIRMED: legacy path, stock reserved here
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SalesOrderStatusManagerImpl implements SalesOrderStatusManager {

    private final InventoryAllocationService inventoryAllocationService;
    private final SalesOrderRepository salesOrderRepository;

    @Override
    public void transition(SalesOrder order, SalesOrderStatus newStatus,
                           Long warehouseId, String username, Locale locale) {

        SalesOrderStatus currentStatus = order.getStatus();

        if (!canTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(String.format(
                    "Invalid status transition from %s to %s", currentStatus, newStatus));
        }

        // Handle inventory and business logic BEFORE status change
        handleStatusTransition(order, currentStatus, newStatus, warehouseId, username, locale);

        // Update status
        order.setStatus(newStatus);

        // Set timestamps based on new status
        if (newStatus == SalesOrderStatus.CONFIRMED) {
            order.setConfirmedAt(LocalDateTime.now());
            order.setConfirmedByUserId(order.getUpdatedByUserId());

            // Ensure fulfillment warehouse is set
            if (order.getFulfillmentWarehouseId() == null) {
                order.setFulfillmentWarehouseId(warehouseId);
            }
        }

        if (newStatus == SalesOrderStatus.APPROVED) {
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedByUserId(order.getUpdatedByUserId());
            order.setApprovalComplete(true);
            if (order.getFulfillmentWarehouseId() == null && warehouseId != null) {
                order.setFulfillmentWarehouseId(warehouseId);
            }
        }

        if (newStatus == SalesOrderStatus.DELIVERED) {
            order.setDeliveredDate(LocalDateTime.now());
        } else if (newStatus == SalesOrderStatus.FULFILLED && order.getDeliveredDate() == null) {
            order.setDeliveredDate(LocalDateTime.now());
        }

        salesOrderRepository.save(order);
        log.info("Updated Sales Order {} status from {} to {}",
                order.getId(), currentStatus, newStatus);
    }

    @Override
    public boolean canTransition(SalesOrderStatus from, SalesOrderStatus to) {
        if (from == to) return true;

        return switch (from) {
            // PAYMENT-GATED FLOW: PENDING_APPROVAL → APPROVED → shipment
            case PENDING_APPROVAL -> to == SalesOrderStatus.APPROVED ||
                    to == SalesOrderStatus.CANCELLED;

            case APPROVED -> to == SalesOrderStatus.PARTIALLY_SHIPPED ||
                    to == SalesOrderStatus.SHIPPED ||
                    to == SalesOrderStatus.CANCELLED;

            // LEGACY BACK-TO-BACK FLOW
            case AWAITING_RECEIPT -> to == SalesOrderStatus.PENDING ||
                    to == SalesOrderStatus.CANCELLED;

            case PENDING -> to == SalesOrderStatus.CONFIRMED ||
                    to == SalesOrderStatus.CANCELLED;

            case CONFIRMED -> to == SalesOrderStatus.PARTIALLY_SHIPPED ||
                    to == SalesOrderStatus.SHIPPED ||
                    to == SalesOrderStatus.CANCELLED;

            // Shared shipment states
            case PARTIALLY_SHIPPED -> to == SalesOrderStatus.SHIPPED;
            case SHIPPED -> to == SalesOrderStatus.DELIVERED;
            case DELIVERED -> to == SalesOrderStatus.FULFILLED;
            case FULFILLED, CANCELLED -> false;
        };
    }

    /**
     * Handles business logic during status transitions
     *
     * CRITICAL: Stock reservation happens here when PENDING → CONFIRMED
     */
    private void handleStatusTransition(SalesOrder order,
                                        SalesOrderStatus from,
                                        SalesOrderStatus to,
                                        Long warehouseId,
                                        String username,
                                        Locale locale) {

        // ========================================
        // STOCK RESERVATION: PENDING → CONFIRMED
        // ========================================
        if (from == SalesOrderStatus.PENDING && to == SalesOrderStatus.CONFIRMED) {

            log.info("SO {} transitioning to CONFIRMED - RESERVING STOCK at warehouse {}",
                    order.getId(), warehouseId);

            // Validate warehouse is set
            if (warehouseId == null && order.getFulfillmentWarehouseId() == null) {
                throw new IllegalStateException(
                        "Cannot confirm Sales Order without fulfillment warehouse");
            }

            Long fulfillmentWarehouse = warehouseId != null ?
                    warehouseId : order.getFulfillmentWarehouseId();

            // Allocate inventory (reserve stock)
            AllocationResult result = inventoryAllocationService
                    .allocateInventory(order, fulfillmentWarehouse);

            if (!result.isSuccess()) {
                throw new IllegalStateException(
                        "Cannot confirm Sales Order - insufficient inventory: " +
                                String.join(", ", result.getErrors()));
            }

            log.info("Successfully reserved stock for Sales Order {} at warehouse {}",
                    order.getId(), fulfillmentWarehouse);
        }

        // ========================================
        // STOCK RELEASE: CONFIRMED → CANCELLED
        // ========================================
        else if (from == SalesOrderStatus.CONFIRMED && to == SalesOrderStatus.CANCELLED) {

            log.info("SO {} being cancelled - RELEASING STOCK RESERVATION", order.getId());

            Long fulfillmentWarehouse = warehouseId != null ?
                    warehouseId : order.getFulfillmentWarehouseId();

            if (fulfillmentWarehouse != null) {
                inventoryAllocationService.releaseAllocation(order, fulfillmentWarehouse);
                log.info("Released stock reservation for cancelled Sales Order {}", order.getId());
            } else {
                log.warn("Cannot release stock for SO {} - no fulfillment warehouse set",
                        order.getId());
            }
        }

        // ========================================
        // DIRECT CANCELLATION: PENDING → CANCELLED
        // ========================================
        else if (from == SalesOrderStatus.PENDING && to == SalesOrderStatus.CANCELLED) {
            // No stock was reserved yet, so nothing to release
            log.info("SO {} cancelled before confirmation - no stock to release", order.getId());
        }

        // ========================================
        // STOCK RESERVATION: APPROVED → PARTIALLY_SHIPPED/SHIPPED (new flow)
        // Stock is reserved at shipment start for payment-gated SOs
        // ========================================
        else if (from == SalesOrderStatus.APPROVED
                && (to == SalesOrderStatus.PARTIALLY_SHIPPED || to == SalesOrderStatus.SHIPPED)) {

            log.info("SO {} transitioning from APPROVED to {} - RESERVING STOCK at warehouse {}",
                    order.getId(), to, warehouseId);

            Long fulfillmentWarehouse = warehouseId != null
                    ? warehouseId
                    : order.getFulfillmentWarehouseId();

            if (fulfillmentWarehouse == null) {
                throw new IllegalStateException(
                        "Cannot start shipment for Sales Order " + order.getId() + " - no fulfillment warehouse set");
            }

            AllocationResult result = inventoryAllocationService.allocateInventory(order, fulfillmentWarehouse);

            if (!result.isSuccess()) {
                throw new IllegalStateException(
                        "Cannot start shipment - insufficient inventory: " +
                                String.join(", ", result.getErrors()));
            }

            if (order.getFulfillmentWarehouseId() == null) {
                order.setFulfillmentWarehouseId(fulfillmentWarehouse);
            }

            log.info("Successfully reserved stock for SO {} at warehouse {}", order.getId(), fulfillmentWarehouse);
        }

        // ========================================
        // STOCK RELEASE: APPROVED → CANCELLED (before shipment)
        // ========================================
        else if (from == SalesOrderStatus.APPROVED && to == SalesOrderStatus.CANCELLED) {
            log.info("SO {} (APPROVED) being cancelled - no stock reserved yet, nothing to release", order.getId());
        }

        // ========================================
        // APPROVAL: PENDING_APPROVAL → APPROVED
        // No stock reservation yet - happens at shipment start
        // ========================================
        else if (from == SalesOrderStatus.PENDING_APPROVAL && to == SalesOrderStatus.APPROVED) {
            log.info("SO {} approved - awaiting shipment dispatch for stock reservation", order.getId());
        }

        // ========================================
        // CANCELLATION FROM PENDING_APPROVAL
        // ========================================
        else if (from == SalesOrderStatus.PENDING_APPROVAL && to == SalesOrderStatus.CANCELLED) {
            log.info("SO {} cancelled from PENDING_APPROVAL - no stock to release", order.getId());
        }

        // ========================================
        // GOODS RECEIVED: AWAITING_RECEIPT → PENDING (legacy)
        // ========================================
        else if (from == SalesOrderStatus.AWAITING_RECEIPT && to == SalesOrderStatus.PENDING) {
            log.info("SO {} goods received - transitioning from AWAITING_RECEIPT to PENDING",
                    order.getId());
        }

        // ========================================
        // CANCELLED BEFORE RECEIPT: AWAITING_RECEIPT → CANCELLED (legacy)
        // ========================================
        else if (from == SalesOrderStatus.AWAITING_RECEIPT && to == SalesOrderStatus.CANCELLED) {
            log.info("SO {} cancelled before goods receipt - no stock to release", order.getId());
        }
    }
}
