package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.PurchaseOrderLine;

import java.util.Locale;

public interface PurchaseOrderLineServiceAuditable {
    PurchaseOrderLine create(PurchaseOrderLine purchaseOrderLine, Locale locale, String username);
    PurchaseOrderLine update(PurchaseOrderLine purchaseOrderLine, Locale locale, String username);
    PurchaseOrderLine delete(PurchaseOrderLine purchaseOrderLine, Locale locale);
}
