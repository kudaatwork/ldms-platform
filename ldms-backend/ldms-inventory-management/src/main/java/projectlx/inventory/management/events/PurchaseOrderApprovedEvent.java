package projectlx.inventory.management.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Locale;

/**
 * Event: Purchase Order Approved
 * 
 * Published when a supplier approves a Purchase Order.
 * Triggers automatic Sales Order creation.
 * 
 * FLOW:
 * PurchaseOrderServiceImpl.update() → status = APPROVED
 *   ↓ (publishes)
 * PurchaseOrderApprovedEvent
 *   ↓ (handled by)
 * PurchaseOrderApprovedEventHandler
 *   ↓ (creates)
 * SalesOrder (PENDING status)
 */
@Getter
public class PurchaseOrderApprovedEvent extends ApplicationEvent {

    private final Long purchaseOrderId;
    private final String purchaseOrderNumber;
    private final Long supplierOrganizationId;
    private final Long approvedByUserId;
    private final Locale locale;

    public PurchaseOrderApprovedEvent(Object source,
                                     Long purchaseOrderId,
                                     String purchaseOrderNumber,
                                     Long supplierOrganizationId,
                                     Long approvedByUserId,
                                     Locale locale) {
        super(source);
        this.purchaseOrderId = purchaseOrderId;
        this.purchaseOrderNumber = purchaseOrderNumber;
        this.supplierOrganizationId = supplierOrganizationId;
        this.approvedByUserId = approvedByUserId;
        this.locale = locale;
    }
}