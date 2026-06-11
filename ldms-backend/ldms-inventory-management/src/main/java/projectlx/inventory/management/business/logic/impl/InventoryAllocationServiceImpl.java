package projectlx.inventory.management.business.logic.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.inventory.management.business.logic.api.InventoryAllocationService;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.InventoryReservation;
import projectlx.inventory.management.model.ReservationStatus;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrderLine;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.repository.InventoryReservationRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class InventoryAllocationServiceImpl implements InventoryAllocationService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AllocationResult allocateInventory(SalesOrder salesOrder, Long warehouseId) {

        List<String> errors = new ArrayList<>();
        List<AllocationDetail> allocations = new ArrayList<>();

        try {

            // First pass: Validate ALL allocations before committing ANY
            for (SalesOrderLine line : salesOrder.getSalesOrderLines()) {

                AllocationDetail allocation = validateAllocation(line, warehouseId);
                allocation.setSalesOrderId(salesOrder.getId());

                if (!allocation.isSuccess()) {
                    errors.add(allocation.getErrorMessage());
                } else {
                    allocations.add(allocation);
                }
            }

            // If any validation failed, return early without any changes
            if (!errors.isEmpty()) {
                return AllocationResult.failure(errors);
            }

            // Second pass: All validations passed, now commit the allocations
            for (AllocationDetail allocation : allocations) {
                commitSingleAllocation(allocation);
            }

            return AllocationResult.success(allocations);

        } catch (Exception e) {
            log.error("Failed to allocate inventory for order {}: {}",
                    salesOrder.getId(), e.getMessage(), e);
            errors.add("Allocation failed: " + e.getMessage());
            return AllocationResult.failure(errors);
        }
    }

    @Override
    @Transactional
    public void releaseAllocation(SalesOrder salesOrder, Long warehouseId) {

        // Only process ACTIVE reservations for this sales order
        List<InventoryReservation> reservations = inventoryReservationRepository
                .findBySalesOrderIdAndStatus(salesOrder.getId(), ReservationStatus.ACTIVE);

        int updatedCount = 0;
        for (InventoryReservation res : reservations) {
            Optional<InventoryItem> itemOpt = findInventoryItem(res.getProductId(), res.getWarehouseLocationId());
            if (itemOpt.isPresent()) {
                InventoryItem item = itemOpt.get();
                BigDecimal currentReserved = item.getReservedQuantity() != null ? item.getReservedQuantity() : BigDecimal.ZERO;
                if (res.getQuantity().compareTo(currentReserved) > 0) {
                    log.warn("Releasing reservation exceeds current reserved: itemId={}, currentReserved={}, releaseQty={}, salesOrderId={}",
                            item.getId(), currentReserved, res.getQuantity(), salesOrder.getId());
                }
                BigDecimal newReserved = currentReserved.subtract(res.getQuantity());
                if (newReserved.compareTo(BigDecimal.ZERO) < 0) newReserved = BigDecimal.ZERO;
                item.setReservedQuantity(newReserved);
                inventoryItemRepository.save(item);
            }

            // Mark reservation as CANCELLED instead of deleting
            res.setStatus(ReservationStatus.CANCELLED);
            inventoryReservationRepository.save(res);
            updatedCount++;
        }

        if (updatedCount > 0) {
            log.info("Released {} reservation(s) (marked CANCELLED) for sales order {}", updatedCount, salesOrder.getId());
        }
    }

    @Override
    public boolean isAllocationPossible(SalesOrder salesOrder, Long warehouseId) {

        for (SalesOrderLine line : salesOrder.getSalesOrderLines()) {
            Optional<InventoryItem> itemOpt = findInventoryItem(line.getProduct().getId(), warehouseId);

            if (itemOpt.isEmpty()) {
                return false;
            }

            InventoryItem item = itemOpt.get();
            BigDecimal availableQuantity = getAvailableQuantity(item);

            if (availableQuantity.compareTo(line.getQuantity()) < 0) {
                return false;
            }
        }

        return true;
    }

    private AllocationDetail validateAllocation(SalesOrderLine line, Long warehouseId) {

        // Use PESSIMISTIC_WRITE lock to prevent race conditions during validation
        Optional<InventoryItem> itemOpt = findInventoryItemWithLock(line.getProduct().getId(), warehouseId);

        if (itemOpt.isEmpty()) {
            return AllocationDetail.failure(line.getId(),
                    "No inventory found for product: " + line.getProduct().getName());
        }

        InventoryItem item = itemOpt.get();
        BigDecimal availableQuantity = getAvailableQuantity(item);

        if (availableQuantity.compareTo(line.getQuantity()) < 0) {
            return AllocationDetail.failure(line.getId(),
                    String.format("Insufficient stock for product %s. Required: %s, Available: %s",
                            line.getProduct().getName(), line.getQuantity(), availableQuantity));
        }

        return AllocationDetail.success(line.getId(), item.getId(), line.getProduct().getId(), warehouseId, line.getQuantity());
    }

    private void commitSingleAllocation(AllocationDetail allocation) {

        // Re-fetch with PESSIMISTIC_WRITE lock to ensure exclusive update
        InventoryItem item = inventoryItemRepository
                .findByProductIdAndWarehouseLocationIdWithLock(allocation.getProductId(), allocation.getWarehouseLocationId(), EntityStatus.DELETED)
                .orElseThrow(() -> new IllegalStateException("Inventory item not found during commit"));

        BigDecimal currentReserved = item.getReservedQuantity() != null ?
                item.getReservedQuantity() : BigDecimal.ZERO;
        BigDecimal newReserved = currentReserved.add(allocation.getQuantity());

        // Double-check availability (race condition protection)
        BigDecimal availableQuantity = getAvailableQuantity(item);

        if (availableQuantity.compareTo(allocation.getQuantity()) < 0) {
            throw new IllegalStateException("Insufficient inventory during commit - possible race condition");
        }

        item.setReservedQuantity(newReserved);
        inventoryItemRepository.save(item);

        // Upsert single reservation row per (sales_order_line_id, product_id, warehouse_location_id)
        Optional<InventoryReservation> existingResOpt = inventoryReservationRepository
                .findBySalesOrderLineIdAndProduct_IdAndWarehouseLocation_Id(
                        allocation.getSalesOrderLineId(),
                        item.getProduct().getId(),
                        item.getWarehouseLocation().getId()
                );

        if (existingResOpt.isPresent()) {
            InventoryReservation existing = existingResOpt.get();
            // If previously CANCELLED/CONSUMED, revive it and set/adjust quantity
            if (existing.getStatus() == ReservationStatus.ACTIVE) {
                existing.setQuantity(existing.getQuantity().add(allocation.getQuantity()));
            } else {
                existing.setStatus(ReservationStatus.ACTIVE);
                existing.setQuantity(allocation.getQuantity());
            }
            inventoryReservationRepository.save(existing);
        } else {
            InventoryReservation reservation = new InventoryReservation();
            reservation.setSalesOrderId(allocation.getSalesOrderId());
            reservation.setSalesOrderLineId(allocation.getSalesOrderLineId());
            reservation.setProduct(item.getProduct());
            reservation.setWarehouseLocation(item.getWarehouseLocation());
            reservation.setQuantity(allocation.getQuantity());
            inventoryReservationRepository.save(reservation);
        }
    }

    private Optional<InventoryItem> findInventoryItem(Long productId, Long warehouseId) {
        return inventoryItemRepository.findByProductIdAndWarehouseLocationIdAndEntityStatusNot(
                productId, warehouseId, EntityStatus.DELETED);
    }

    private Optional<InventoryItem> findInventoryItemWithLock(Long productId, Long warehouseId) {
        return inventoryItemRepository.findByProductIdAndWarehouseLocationIdWithLock(
                productId, warehouseId, EntityStatus.DELETED);
    }

    private BigDecimal getAvailableQuantity(InventoryItem item) {
        BigDecimal currentStock = item.getCurrentStock() != null ? item.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal reserved = item.getReservedQuantity() != null ? item.getReservedQuantity() : BigDecimal.ZERO;
        BigDecimal available = currentStock.subtract(reserved);
        return available.max(BigDecimal.ZERO); // Never return negative
    }

    // Inner classes remain the same
    public static class AllocationResult {

        private final boolean success;
        private final List<String> errors;
        private final List<AllocationDetail> allocations;

        private AllocationResult(boolean success, List<String> errors, List<AllocationDetail> allocations) {
            this.success = success;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.allocations = allocations != null ? allocations : new ArrayList<>();
        }

        public static AllocationResult success(List<AllocationDetail> allocations) {
            return new AllocationResult(true, null, allocations);
        }

        public static AllocationResult failure(List<String> errors) {
            return new AllocationResult(false, errors, null);
        }

        public boolean isSuccess() { return success; }
        public List<String> getErrors() { return errors; }
        public List<AllocationDetail> getAllocations() { return allocations; }
    }

    public static class AllocationDetail {

        private final boolean success;
        private final Long salesOrderLineId;
        private final Long inventoryItemId;
        private final Long productId;
        private final Long warehouseLocationId;
        private final BigDecimal quantity;
        private final String errorMessage;
        private Long salesOrderId;

        private AllocationDetail(boolean success,
                                 Long salesOrderLineId,
                                 Long inventoryItemId,
                                 Long productId,
                                 Long warehouseLocationId,
                                 BigDecimal quantity,
                                 String errorMessage) {
            this.success = success;
            this.salesOrderLineId = salesOrderLineId;
            this.inventoryItemId = inventoryItemId;
            this.productId = productId;
            this.warehouseLocationId = warehouseLocationId;
            this.quantity = quantity;
            this.errorMessage = errorMessage;
        }

        public static AllocationDetail success(Long salesOrderLineId,
                                               Long inventoryItemId,
                                               Long productId,
                                               Long warehouseLocationId,
                                               BigDecimal quantity) {
            return new AllocationDetail(true, salesOrderLineId, inventoryItemId, productId, warehouseLocationId, quantity, null);
        }

        public static AllocationDetail failure(Long salesOrderLineId, String errorMessage) {
            return new AllocationDetail(false, salesOrderLineId, null, null, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public Long getSalesOrderLineId() { return salesOrderLineId; }
        public Long getInventoryItemId() { return inventoryItemId; }
        public Long getProductId() { return productId; }
        public Long getWarehouseLocationId() { return warehouseLocationId; }
        public BigDecimal getQuantity() { return quantity; }
        public String getErrorMessage() { return errorMessage; }
        public Long getSalesOrderId() { return salesOrderId; }
        public void setSalesOrderId(Long salesOrderId) { this.salesOrderId = salesOrderId; }
    }
}