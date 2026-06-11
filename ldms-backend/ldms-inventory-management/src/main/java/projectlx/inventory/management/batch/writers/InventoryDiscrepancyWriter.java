package projectlx.inventory.management.batch.writers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import projectlx.inventory.management.model.InventoryDiscrepancy;
import projectlx.inventory.management.batch.service.NotificationService;
import projectlx.inventory.management.repository.InventoryDiscrepancyRepository;

@RequiredArgsConstructor
@Slf4j
public class InventoryDiscrepancyWriter implements ItemWriter<InventoryDiscrepancy> {

    private final InventoryDiscrepancyRepository discrepancyRepository;
    private final NotificationService notificationService;

    @Override
    public void write(Chunk<? extends InventoryDiscrepancy> chunk) throws Exception {
        for (InventoryDiscrepancy discrepancy : chunk) {
            // Fixed: Added persistence and notification
            discrepancyRepository.save(discrepancy);
            notificationService.sendDiscrepancyAlert(discrepancy);

            log.error("INVENTORY DISCREPANCY: Item {} expected {}, actual {}, variance {}",
                    discrepancy.getInventoryItemId(), discrepancy.getExpectedStock(),
                    discrepancy.getActualStock(), discrepancy.getVariance());
        }
    }
}