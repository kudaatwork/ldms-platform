package projectlx.billing.payments.events.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.billing.payments.business.logic.api.InvoiceService;
import projectlx.billing.payments.utils.config.RabbitMQConsumerConfig;

import java.util.Locale;
import java.util.Map;

/**
 * Listens to the inventory po.approved event and auto-generates a
 * PURCHASE_ORDER invoice in billing when a purchase order is approved.
 *
 * FLOW:
 * inventory-management → po.approved routing key → billing.po.approved.queue
 *   ↓ (consumed by)
 * PoApprovedInvoiceHandler
 *   ↓ (delegates to)
 * InvoiceService.generateFromPurchaseOrderEvent(event)
 *   ↓ (persists)
 * Invoice (sourceType = PURCHASE_ORDER)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PoApprovedInvoiceHandler {

    private final InvoiceService invoiceService;

    @RabbitListener(queues = RabbitMQConsumerConfig.PO_APPROVED_QUEUE)
    public void handlePoApprovedEvent(Map<String, Object> event) {
        if (event == null || !event.containsKey("purchaseOrderId")) {
            log.debug("PO approved event missing purchaseOrderId; ignoring for billing.");
            return;
        }
        try {
            invoiceService.generateFromPurchaseOrderEvent(event, Locale.ENGLISH);
        } catch (Exception ex) {
            log.error("Failed to generate invoice from PO approved event: {}", ex.getMessage(), ex);
        }
    }
}
