package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.PurchaseOrder;

import java.util.Locale;

public interface PurchaseOrderServiceAuditable {
    PurchaseOrder create(PurchaseOrder purchaseOrder, Locale locale, String username);
    PurchaseOrder update(PurchaseOrder purchaseOrder, Locale locale, String username);
    PurchaseOrder delete(PurchaseOrder purchaseOrder, Locale locale);
}
