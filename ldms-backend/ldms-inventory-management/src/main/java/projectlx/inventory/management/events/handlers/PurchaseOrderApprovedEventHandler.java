package projectlx.inventory.management.events.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.events.PurchaseOrderApprovedEvent;

/**
 * Event Handler: PO Approved (Internal Spring ApplicationEvent)
 *
 * PURPOSE:
 * Logs the PO approval event for audit/debugging purposes.
 *
 * SO CREATION NOTE:
 * Sales Order creation is NO LONGER triggered here. Instead, SOs are created
 * by the PaymentVerifiedEventHandler after billing confirms payment for the PO.
 * This ensures an SO is only created after financial commitment is confirmed.
 *
 * FLOW (new):
 * 1. Supplier approves PO via ProcurementWorkflowService → RabbitMQ po.approved event published
 * 2. Billing service listens on po.approved → creates invoice
 * 3. Customer pays invoice → billing publishes billing.payment.verified
 * 4. PaymentVerifiedEventHandler creates SO in PENDING_APPROVAL status
 * 5. SO approval (ProcurementWorkflowService.approveSalesOrderStage) → APPROVED
 */
@Component
@Slf4j
public class PurchaseOrderApprovedEventHandler {

    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePurchaseOrderApproved(PurchaseOrderApprovedEvent event) {
        log.info("PO approved event received for PO ID: {} - SO creation deferred until payment confirmed",
                event.getPurchaseOrderId());

        // SO creation is handled by PaymentVerifiedEventHandler after billing confirms payment.
        // No action needed here beyond logging.
    }
}