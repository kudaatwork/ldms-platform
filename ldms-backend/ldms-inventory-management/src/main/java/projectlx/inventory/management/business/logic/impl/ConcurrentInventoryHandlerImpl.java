package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.logic.api.ConcurrentInventoryHandler;
import projectlx.inventory.management.exceptions.InsufficientInventoryException;
import projectlx.inventory.management.exceptions.InventoryNotFoundException;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.repository.InventoryItemRepository;
import projectlx.inventory.management.utils.CostCalculator;
import java.math.BigDecimal;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class ConcurrentInventoryHandlerImpl implements ConcurrentInventoryHandler {

    private final InventoryItemRepository inventoryItemRepository;
    private final CostCalculator costCalculator;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 10)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public InventoryItem updateInventoryWithLocking(Long inventoryItemId,
                                                   BigDecimal quantityChange,
                                                   BigDecimal unitCost,
                                                   String operation) {

        // Use pessimistic locking to prevent concurrent modifications
        Optional<InventoryItem> itemOpt = inventoryItemRepository.findByIdWithLock(inventoryItemId);

        if (itemOpt.isEmpty()) {
            throw new InventoryNotFoundException("Inventory item not found: " + inventoryItemId);
        }

        InventoryItem item = itemOpt.get();

        // Validate operation
        if ("STOCK_OUT".equals(operation)) {
            BigDecimal availableQuantity = getAvailableQuantity(item);
            if (availableQuantity.compareTo(quantityChange.abs()) < 0) {
                throw new InsufficientInventoryException(
                    String.format("Insufficient inventory. Available: %s, Required: %s",
                        availableQuantity, quantityChange.abs()));
            }
        }

        // Apply changes
        updateInventoryQuantities(item, quantityChange, unitCost, operation);

        return inventoryItemRepository.save(item);
    }

    private void updateInventoryQuantities(InventoryItem item, BigDecimal quantityChange,
                                           BigDecimal unitCost, String operation) {

        BigDecimal currentQuantity = item.getQuantity() != null ?
                item.getQuantity() : BigDecimal.ZERO;
        BigDecimal currentStock = item.getCurrentStock() != null ?
                item.getCurrentStock() : BigDecimal.ZERO;

        if ("STOCK_IN".equals(operation)) {
            // STOCK_IN: Add inventory (e.g., Goods Receipt)
            // quantityChange is positive (e.g., +150)
            // Result: currentQuantity + quantityChange

            // Calculate new weighted average cost
            BigDecimal newAverageCost = costCalculator.calculateWeightedAverageCost(
                    item, quantityChange, unitCost);

            item.setQuantity(currentQuantity.add(quantityChange));
            item.setCurrentStock(currentStock.add(quantityChange));
            item.setAverageCost(newAverageCost);

            BigDecimal newTotalCost = item.getTotalCost() != null ?
                    item.getTotalCost().add(quantityChange.multiply(unitCost)) :
                    quantityChange.multiply(unitCost);
            item.setTotalCost(newTotalCost);

            log.info("STOCK_IN recorded: quantity {} + {} = {}, total_cost {} + {} = {}",
                    currentQuantity, quantityChange,
                    currentQuantity.add(quantityChange),
                    item.getTotalCost(), quantityChange.multiply(unitCost),
                    newTotalCost);

        } else if ("STOCK_OUT".equals(operation)) {
            // STOCK_OUT: Subtract inventory (e.g., Purchase Return, Sales)
            // quantityChange is NEGATIVE (e.g., -5)
            // Mathematical calculation: currentQuantity - 5 = currentQuantity + (-5)
            // So we use: currentQuantity.add(quantityChange)
            // This is because add(-5) = subtract(5) ✅

            // Calculate outgoing value using absolute value
            // Example: |-5| * 65.50 = 5 * 65.50 = 327.50
            BigDecimal outgoingValue = quantityChange.abs().multiply(item.getAverageCost());

            // ✅ Use add() instead of subtract() for negative quantityChange
            // 200 + (-5) = 195 ✅
            item.setQuantity(currentQuantity.add(quantityChange));
            item.setCurrentStock(currentStock.add(quantityChange));

            // Subtract the outgoing cost value
            BigDecimal newTotalCost = item.getTotalCost() != null ?
                    item.getTotalCost().subtract(outgoingValue) : BigDecimal.ZERO;
            item.setTotalCost(newTotalCost.max(BigDecimal.ZERO));

            log.info("STOCK_OUT recorded: quantity {} + {} = {}, total_cost {} - {} = {}",
                    currentQuantity, quantityChange,
                    currentQuantity.add(quantityChange),
                    item.getTotalCost(), outgoingValue,
                    newTotalCost);
        }
    }

    private BigDecimal getAvailableQuantity(InventoryItem item) {
        BigDecimal currentStock = item.getCurrentStock() != null ? 
            item.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal reserved = item.getReservedQuantity() != null ? 
            item.getReservedQuantity() : BigDecimal.ZERO;
        return currentStock.subtract(reserved).max(BigDecimal.ZERO);
    }
}