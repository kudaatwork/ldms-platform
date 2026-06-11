package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import java.util.Locale;

public interface PurchaseOrderStatusManager {
    void transition(PurchaseOrder order, PurchaseOrderStatus targetStatus,
                    String username, Locale locale);
    boolean canTransition(PurchaseOrderStatus current, PurchaseOrderStatus target);
}
