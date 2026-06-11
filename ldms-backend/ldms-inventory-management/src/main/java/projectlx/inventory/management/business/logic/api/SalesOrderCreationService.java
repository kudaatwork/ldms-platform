package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.model.SalesOrder;

import java.util.Locale;

/**
 * Service responsible for creating Sales Orders from approved Purchase Orders.
 *
 * FLOW:
 * 1. PO gets APPROVED by supplier
 * 2. This service creates corresponding SO in PENDING status
 * 3. SO contains all PO details from supplier's perspective
 * 4. Supplier can then CONFIRM SO → triggers stock reservation
 */
public interface SalesOrderCreationService {

    /**
     * Creates a Sales Order from an approved Purchase Order.
     *
     * @param purchaseOrder   The approved PO
     * @param supplierOrgId   The supplier organization ID
     * @param createdByUserId User creating the SO
     * @param locale          User locale
     * @return The created Sales Order in PENDING status
     */
    SalesOrder createFromPurchaseOrder(PurchaseOrder purchaseOrder,
                                       Long supplierOrgId,
                                       Long createdByUserId,
                                       Locale locale);
}