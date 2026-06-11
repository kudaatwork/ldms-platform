package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.StockTransactionHistory;

import java.util.Locale;

public interface StockTransactionHistoryServiceAuditable {
    StockTransactionHistory create(StockTransactionHistory stockTransactionHistory, Locale locale, String username);
    StockTransactionHistory update(StockTransactionHistory stockTransactionHistory, Locale locale, String username);
    StockTransactionHistory delete(StockTransactionHistory stockTransactionHistory, Locale locale);
}
