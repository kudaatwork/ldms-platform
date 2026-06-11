package projectlx.inventory.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.InventoryAllocationService;
import projectlx.inventory.management.business.logic.impl.InventoryAllocationServiceImpl.AllocationResult;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.service.processor.api.InventoryAllocationServiceProcessor;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InventoryAllocationServiceProcessorImpl implements InventoryAllocationServiceProcessor {

    private final InventoryAllocationService inventoryAllocationService;
    private static final Logger logger = LoggerFactory.getLogger(InventoryAllocationServiceProcessorImpl.class);

    @Override
    public AllocationResult allocateInventory(SalesOrder salesOrder, Long warehouseId) {
        logger.info("Incoming request to allocate inventory for sales order ID: {} at warehouse ID: {}", 
                salesOrder != null ? salesOrder.getId() : null, warehouseId);

        AllocationResult result = inventoryAllocationService.allocateInventory(salesOrder, warehouseId);

        logger.info("Outgoing response after allocating inventory: Success: {}", 
                result != null ? result.isSuccess() : false);

        return result;
    }

    @Override
    public void releaseAllocation(SalesOrder salesOrder, Long warehouseId) {
        logger.info("Incoming request to release allocation for sales order ID: {} at warehouse ID: {}", 
                salesOrder != null ? salesOrder.getId() : null, warehouseId);

        inventoryAllocationService.releaseAllocation(salesOrder, warehouseId);

        logger.info("Allocation released successfully for sales order ID: {} at warehouse ID: {}", 
                salesOrder != null ? salesOrder.getId() : null, warehouseId);
    }

    @Override
    public boolean isAllocationPossible(SalesOrder salesOrder, Long warehouseId) {
        logger.info("Incoming request to check if allocation is possible for sales order ID: {} at warehouse ID: {}", 
                salesOrder != null ? salesOrder.getId() : null, warehouseId);

        boolean possible = inventoryAllocationService.isAllocationPossible(salesOrder, warehouseId);

        logger.info("Outgoing response after checking allocation possibility: {}", possible);

        return possible;
    }

    @Override
    public boolean isAllocationPossible(Long productId, Long warehouseId, BigDecimal requiredQuantity) {
        logger.info("Incoming request to check if allocation is possible for product ID: {} at warehouse ID: {} with quantity: {}", 
                productId, warehouseId, requiredQuantity);

        boolean possible = inventoryAllocationService.isAllocationPossible(productId, warehouseId, requiredQuantity);

        logger.info("Outgoing response after checking allocation possibility for product: {}", possible);

        return possible;
    }

    @Override
    public BigDecimal getAvailableQuantity(Long productId, Long warehouseId) {
        logger.info("Incoming request to get available quantity for product ID: {} at warehouse ID: {}", 
                productId, warehouseId);

        BigDecimal quantity = inventoryAllocationService.getAvailableQuantity(productId, warehouseId);

        logger.info("Outgoing response with available quantity: {}", quantity);

        return quantity;
    }

    @Override
    public BigDecimal getReservedQuantity(Long productId, Long warehouseId) {
        logger.info("Incoming request to get reserved quantity for product ID: {} at warehouse ID: {}", 
                productId, warehouseId);

        BigDecimal quantity = inventoryAllocationService.getReservedQuantity(productId, warehouseId);

        logger.info("Outgoing response with reserved quantity: {}", quantity);

        return quantity;
    }
}