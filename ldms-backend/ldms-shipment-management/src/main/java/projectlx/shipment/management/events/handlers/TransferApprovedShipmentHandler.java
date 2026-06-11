package projectlx.shipment.management.events.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.shipment.management.business.logic.api.ShipmentService;
import projectlx.shipment.management.utils.config.RabbitMQConsumerConfig;

import java.util.Locale;
import java.util.Map;

/**
 * Listens to the inventory transfer.approved event and auto-creates a shipment
 * in PENDING_ALLOCATION state for the approved inventory transfer.
 *
 * FLOW:
 * inventory-management → inventory.transfer.approved routing key → shipment.transfer.approved.queue
 *   ↓ (consumed by)
 * TransferApprovedShipmentHandler
 *   ↓ (delegates to)
 * ShipmentService.createFromTransferApprovedEvent(event)
 *   ↓ (persists, idempotent)
 * Shipment (status = PENDING_ALLOCATION)
 *
 * Expected payload fields:
 * - transferId (Long, required)
 * - transferNumber (String)
 * - organizationId (Long)
 * - fromWarehouseLocationId (Long)
 * - toWarehouseLocationId (Long)
 * - fromWarehouseName (String)
 * - toWarehouseName (String)
 * - productId (Long)
 * - productName (String)
 * - productCode (String)
 * - quantity (BigDecimal)
 * - approvedByUserId (Long)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferApprovedShipmentHandler {

    private final ShipmentService shipmentService;

    @RabbitListener(queues = RabbitMQConsumerConfig.TRANSFER_APPROVED_QUEUE)
    public void handleTransferApprovedEvent(Map<String, Object> event) {
        if (event == null || !event.containsKey("transferId")) {
            log.debug("inventory.transfer.approved event missing transferId; ignoring for shipment.");
            return;
        }
        log.info("Received inventory.transfer.approved event for transferId={}",
                event.get("transferId"));
        try {
            shipmentService.createFromTransferApprovedEvent(event, Locale.ENGLISH);
        } catch (Exception ex) {
            log.error("Failed to create shipment from transfer approved event transferId={}: {}",
                    event.get("transferId"), ex.getMessage(), ex);
            throw ex;
        }
    }
}
