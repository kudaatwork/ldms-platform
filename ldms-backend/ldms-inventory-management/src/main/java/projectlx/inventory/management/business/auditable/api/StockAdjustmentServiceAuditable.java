package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.StockAdjustment;

import java.util.Locale;

public interface StockAdjustmentServiceAuditable {
    StockAdjustment create(StockAdjustment stockAdjustment, Locale locale, String username);
    StockAdjustment update(StockAdjustment stockAdjustment, Locale locale, String username);
    StockAdjustment delete(StockAdjustment stockAdjustment, Locale locale);
}
