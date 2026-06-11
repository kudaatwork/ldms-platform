package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.PurchaseReturn;

import java.util.Locale;

public interface PurchaseReturnServiceAuditable {
    PurchaseReturn create(PurchaseReturn purchaseReturn, Locale locale, String username);
    PurchaseReturn update(PurchaseReturn purchaseReturn, Locale locale, String username);
    PurchaseReturn delete(PurchaseReturn purchaseReturn, Locale locale);
}