package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.StockTransactionHistoryServiceAuditable;
import projectlx.inventory.management.model.StockTransactionHistory;
import projectlx.inventory.management.repository.StockTransactionHistoryRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StockTransactionHistoryServiceAuditableImpl implements StockTransactionHistoryServiceAuditable {

    private final StockTransactionHistoryRepository stockTransactionHistoryRepository;

    @Override
    public StockTransactionHistory create(StockTransactionHistory stockTransactionHistory, Locale locale, String username) {
        return stockTransactionHistoryRepository.save(stockTransactionHistory);
    }

    @Override
    public StockTransactionHistory update(StockTransactionHistory stockTransactionHistory, Locale locale, String username) {
        return stockTransactionHistoryRepository.save(stockTransactionHistory);
    }

    @Override
    public StockTransactionHistory delete(StockTransactionHistory stockTransactionHistory, Locale locale) {
        return stockTransactionHistoryRepository.save(stockTransactionHistory);
    }
}
