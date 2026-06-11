package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.model.InventoryItem;
import java.math.BigDecimal;

public interface ConcurrentInventoryHandler {
    InventoryItem updateInventoryWithLocking(Long inventoryItemId, 
                                           BigDecimal quantityChange,
                                           BigDecimal unitCost,
                                           String operation);
}