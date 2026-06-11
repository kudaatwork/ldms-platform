package projectlx.inventory.management.batch.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import projectlx.inventory.management.model.InventoryDiscrepancy;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.inventory.management.model.StockTransactionHistory;
import projectlx.inventory.management.repository.StockTransactionHistoryRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
public class InventoryReconciliationProcessor implements ItemProcessor<InventoryItem, InventoryDiscrepancy> {

    private final StockTransactionHistoryRepository transactionHistoryRepository;
    private final BigDecimal varianceThreshold;

    @Override
    public InventoryDiscrepancy process(InventoryItem item) throws Exception {
        log.debug("Reconciling inventory for item: {}", item.getId());

        BigDecimal calculatedStock = calculateStockFromTransactions(item);
        BigDecimal actualStock = item.getCurrentStock();
        BigDecimal variance = actualStock.subtract(calculatedStock);

        if (variance.abs().compareTo(varianceThreshold) > 0) {
            InventoryDiscrepancy discrepancy = new InventoryDiscrepancy();
            discrepancy.setInventoryItemId(item.getId());
            discrepancy.setProductId(item.getProduct().getId());
            discrepancy.setWarehouseLocationId(item.getWarehouseLocation().getId());
            discrepancy.setExpectedStock(calculatedStock);
            discrepancy.setActualStock(actualStock);
            discrepancy.setVariance(variance);
            discrepancy.setDiscoveredAt(LocalDateTime.now());
            return discrepancy;
        }

        return null;
    }

    private BigDecimal calculateStockFromTransactions(InventoryItem item) {
        return transactionHistoryRepository
                .findByInventoryItemIdAndEntityStatusNot(item.getId(), EntityStatus.DELETED)
                .stream()
                .map(StockTransactionHistory::getQuantityChange)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}