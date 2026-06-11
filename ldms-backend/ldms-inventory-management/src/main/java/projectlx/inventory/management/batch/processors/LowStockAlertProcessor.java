package projectlx.inventory.management.batch.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.LowStockAlert;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
public class LowStockAlertProcessor implements ItemProcessor<InventoryItem, LowStockAlert> {

    @Override
    public LowStockAlert process(InventoryItem item) throws Exception {
        log.info("Processing low stock alert for product: {}", item.getProduct().getName());

        LowStockAlert alert = new LowStockAlert();
        alert.setInventoryItemId(item.getId());
        alert.setProductId(item.getProduct().getId());
        alert.setProductName(item.getProduct().getName());
        alert.setWarehouseLocationId(item.getWarehouseLocation().getId());
        alert.setCurrentStock(item.getCurrentStock());
        alert.setMinStockLevel(item.getMinStockLevel());
        alert.setReorderQuantity(item.getReorderQuantity());
        alert.setAlertDate(LocalDateTime.now());
        alert.setSeverity(calculateSeverity(item));

        return alert;
    }

    private String calculateSeverity(InventoryItem item) {
        if (item.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0) {
            return "CRITICAL";
        } else if (item.getCurrentStock().compareTo(item.getMinStockLevel().multiply(BigDecimal.valueOf(0.5))) <= 0) {
            return "HIGH";
        } else {
            return "MEDIUM";
        }
    }
}