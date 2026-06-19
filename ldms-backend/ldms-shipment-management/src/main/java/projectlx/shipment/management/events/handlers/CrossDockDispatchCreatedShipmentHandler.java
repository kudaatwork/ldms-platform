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
 * Listens to the cross.dock.dispatch.created event and auto-creates a shipment
 * in PENDING_ALLOCATION state for the incoming cross-dock dispatch.
 *
 * FLOW:
 * inventory-management → cross.dock.dispatch.created routing key → shipment.cross.dock.dispatch.created.queue
 *   ↓ (consumed by)
 * CrossDockDispatchCreatedShipmentHandler
 *   ↓ (delegates to)
 * ShipmentService.createFromCrossDockDispatchCreatedEvent(event)
 *   ↓ (persists, idempotent)
 * Shipment (sourceType = CROSS_DOCK_DISPATCH, status = PENDING_ALLOCATION)
 *
 * Expected payload fields:
 * - dispatchId (Long, required)
 * - organizationId (Long)
 * - externalDispatchId (String)
 * - productCode (String)
 * - quantity (BigDecimal)
 * - fromLocationLabel (String)
 * - toLocationLabel (String)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CrossDockDispatchCreatedShipmentHandler {

    private final ShipmentService shipmentService;

    @RabbitListener(queues = RabbitMQConsumerConfig.CROSS_DOCK_DISPATCH_CREATED_QUEUE)
    public void handleCrossDockDispatchCreatedEvent(Map<String, Object> event) {
        if (event == null || !event.containsKey("dispatchId")) {
            log.debug("cross.dock.dispatch.created event missing dispatchId; ignoring for shipment.");
            return;
        }
        log.info("Received cross.dock.dispatch.created event for dispatchId={}",
                event.get("dispatchId"));
        try {
            shipmentService.createFromCrossDockDispatchCreatedEvent(event, Locale.ENGLISH);
        } catch (Exception ex) {
            log.error("Failed to create shipment from cross.dock.dispatch.created event dispatchId={}: {}",
                    event.get("dispatchId"), ex.getMessage(), ex);
            throw ex;
        }
    }
}
