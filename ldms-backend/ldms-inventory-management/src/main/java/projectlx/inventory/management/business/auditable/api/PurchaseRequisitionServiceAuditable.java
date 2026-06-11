package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.PurchaseRequisition;
import projectlx.inventory.management.model.PurchaseRequisitionAmendment;

import java.util.Locale;

public interface PurchaseRequisitionServiceAuditable {
    PurchaseRequisition create(PurchaseRequisition purchaseRequisition, Locale locale, String username);
    PurchaseRequisition update(PurchaseRequisition purchaseRequisition, Locale locale, String username);
    PurchaseRequisition delete(PurchaseRequisition purchaseRequisition, Locale locale);
    PurchaseRequisitionAmendment createAmendment(PurchaseRequisitionAmendment amendment, Locale locale, String username);
}
