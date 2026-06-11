package projectlx.inventory.management.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.inventory.management.model.InventoryItem;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class CostCalculator {

    private static final int COST_PRECISION = 4;
    private static final RoundingMode COST_ROUNDING = RoundingMode.HALF_UP;

    public BigDecimal calculateWeightedAverageCost(InventoryItem existingItem, 
                                                  BigDecimal newQuantity, 
                                                  BigDecimal newUnitCost) {
        
        BigDecimal existingQuantity = existingItem.getCurrentStock() != null ? 
            existingItem.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal existingTotalCost = existingItem.getTotalCost() != null ? 
            existingItem.getTotalCost() : BigDecimal.ZERO;
        
        // Calculate new total cost
        BigDecimal newTotalCost = newQuantity.multiply(newUnitCost);
        BigDecimal combinedTotalCost = existingTotalCost.add(newTotalCost);
        BigDecimal combinedQuantity = existingQuantity.add(newQuantity);
        
        // Avoid division by zero
        if (combinedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate weighted average with proper precision
        BigDecimal weightedAverage = combinedTotalCost.divide(combinedQuantity, 
            COST_PRECISION, COST_ROUNDING);
        
        log.debug("WAC calculation - Existing: {}, New: {}, Result: {}", 
            existingItem.getAverageCost(), newUnitCost, weightedAverage);
        
        return weightedAverage;
    }

    public BigDecimal calculateTotalCost(BigDecimal quantity, BigDecimal unitCost) {
        if (quantity == null || unitCost == null) {
            return BigDecimal.ZERO;
        }
        return quantity.multiply(unitCost).setScale(COST_PRECISION, COST_ROUNDING);
    }
}