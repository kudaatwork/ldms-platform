package projectlx.inventory.management.service.processor.api;

import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.business.logic.impl.InventoryAllocationServiceImpl.AllocationResult;

/**
 * Processor interface for managing inventory allocation and reservations.
 * Handles the allocation of inventory to sales orders to prevent overselling.
 */
public interface InventoryAllocationServiceProcessor {

    /**
     * Allocates inventory for a sales order at a specific warehouse.
     * This method reserves the required inventory quantities for all line items in the order.
     *
     * @param salesOrder the sales order requiring inventory allocation
     * @param warehouseId the warehouse from which to allocate inventory
     * @return AllocationResult containing success status and details of the allocation
     * @throws IllegalArgumentException if salesOrder is null or warehouseId is null
     */
    AllocationResult allocateInventory(SalesOrder salesOrder, Long warehouseId);

    /**
     * Releases (unreserves) inventory allocation for a sales order.
     * This method should be called when an order is cancelled or modified.
     *
     * @param salesOrder the sales order for which to release reservations
     * @param warehouseId the warehouse from which inventory was allocated
     * @throws IllegalArgumentException if salesOrder is null or warehouseId is null
     */
    void releaseAllocation(SalesOrder salesOrder, Long warehouseId);

    /**
     * Checks if inventory allocation is possible for a sales order without actually allocating.
     * Useful for pre-validation before confirming orders.
     *
     * @param salesOrder the sales order to check
     * @param warehouseId the warehouse to check availability in
     * @return true if all items in the order can be allocated, false otherwise
     * @throws IllegalArgumentException if salesOrder is null or warehouseId is null
     */
    boolean isAllocationPossible(SalesOrder salesOrder, Long warehouseId);

    /**
     * Checks if inventory allocation is possible for a specific quantity of a product.
     * Useful for real-time availability checks during order entry.
     *
     * @param productId the product to check
     * @param warehouseId the warehouse to check
     * @param requiredQuantity the quantity needed
     * @return true if the required quantity can be allocated, false otherwise
     */
    boolean isAllocationPossible(Long productId, Long warehouseId, java.math.BigDecimal requiredQuantity);

    /**
     * Gets the currently available (unreserved) quantity for a product at a warehouse.
     *
     * @param productId the product to check
     * @param warehouseId the warehouse to check
     * @return the available quantity, or zero if product not found
     */
    java.math.BigDecimal getAvailableQuantity(Long productId, Long warehouseId);

    /**
     * Gets the currently reserved quantity for a product at a warehouse.
     * Useful for reporting and monitoring purposes.
     *
     * @param productId the product to check
     * @param warehouseId the warehouse to check
     * @return the reserved quantity, or zero if product not found
     */
    java.math.BigDecimal getReservedQuantity(Long productId, Long warehouseId);
}