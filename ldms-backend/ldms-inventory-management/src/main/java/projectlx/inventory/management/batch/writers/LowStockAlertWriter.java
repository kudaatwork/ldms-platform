package projectlx.inventory.management.batch.writers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import projectlx.inventory.management.batch.service.NotificationService;
import projectlx.inventory.management.model.LowStockAlert;

@RequiredArgsConstructor
@Slf4j
public class LowStockAlertWriter implements ItemWriter<LowStockAlert> {

    private final NotificationService notificationService;

    @Override
    public void write(Chunk<? extends LowStockAlert> chunk) throws Exception {
        for (LowStockAlert alert : chunk) {
            notificationService.sendLowStockAlert(alert);

            log.warn("LOW STOCK ALERT: Product {} (ID: {}) at warehouse {} has {} units, minimum is {}",
                    alert.getProductName(), alert.getProductId(), alert.getWarehouseLocationId(),
                    alert.getCurrentStock(), alert.getMinStockLevel());
        }
    }
}