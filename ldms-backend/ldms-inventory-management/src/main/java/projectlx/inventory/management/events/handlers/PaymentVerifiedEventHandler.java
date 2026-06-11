package projectlx.inventory.management.events.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.logic.api.ProcurementWorkflowService;
import projectlx.inventory.management.utils.config.RabbitMQConsumerConfig;

import java.util.Locale;
import java.util.Map;

/**
 * RabbitMQ Consumer: billing.payment.verified → Create Sales Order
 *
 * PURPOSE:
 * Listens on the billing.payment.verified queue. When billing confirms that
 * a payment for a Purchase Order has been verified, this handler creates
 * the corresponding Sales Order in PENDING_APPROVAL status.
 *
 * FLOW:
 * 1. Supplier approves PO → po.approved event → billing creates invoice
 * 2. Customer pays invoice → billing publishes billing.payment.verified
 * 3. This handler receives the event
 * 4. Calls ProcurementWorkflowService.createSalesOrderFromPaidPurchaseOrder
 * 5. SO created in PENDING_APPROVAL status
 * 6. SO approval workflow → APPROVED → stock reserved at shipment start
 *
 * MESSAGE FORMAT (expected from billing service):
 * {
 *   "purchaseOrderId": 123,
 *   "supplierOrganizationId": 456,
 *   "paidByUserId": 789,
 *   "invoiceId": 999
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentVerifiedEventHandler {

    private final ProcurementWorkflowService procurementWorkflowService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConsumerConfig.BILLING_PAYMENT_VERIFIED_QUEUE)
    @Transactional
    public void handlePaymentVerified(Map<String, Object> payload) {
        log.info("Received billing.payment.verified event: {}", payload);

        try {
            Long purchaseOrderId = extractLong(payload, "purchaseOrderId");
            Long supplierOrganizationId = extractLong(payload, "supplierOrganizationId");
            Long paidByUserId = extractLong(payload, "paidByUserId");

            if (purchaseOrderId == null) {
                log.error("billing.payment.verified event missing purchaseOrderId - skipping: {}", payload);
                return;
            }

            log.info("Creating SO from paid PO: {} for supplier org: {}", purchaseOrderId, supplierOrganizationId);

            procurementWorkflowService.createSalesOrderFromPaidPurchaseOrder(
                    purchaseOrderId,
                    supplierOrganizationId,
                    paidByUserId,
                    Locale.ENGLISH,
                    "system:billing-payment-verified"
            );

            log.info("Successfully created SO for paid PO: {}", purchaseOrderId);

        } catch (Exception ex) {
            log.error("Failed to process billing.payment.verified event: {} - error: {}",
                    payload, ex.getMessage(), ex);
            // Re-throw to trigger DLQ routing if applicable
            throw new RuntimeException("Failed to process payment verified event", ex);
        }
    }

    private Long extractLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Cannot parse {} as Long from payment verified event: {}", key, value);
            return null;
        }
    }
}
