package projectlx.trip.tracking.events.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.trip.tracking.utils.config.RabbitMQConsumerConfig;

import java.util.Map;

/**
 * Listens for shipment.allocated events from the shipment exchange.
 *
 * Currently log-only — a pre-scheduled trip may be created here in a future iteration.
 */
@Component
@Slf4j
public class ShipmentAllocatedEventHandler {

    @RabbitListener(queues = RabbitMQConsumerConfig.SHIPMENT_ALLOCATED_QUEUE)
    public void handleShipmentAllocated(Map<String, Object> payload) {
        try {
            log.info("Received shipment.allocated event: shipmentId={} orgId={}",
                    payload.get("shipmentId"), payload.get("organizationId"));
        } catch (Exception ex) {
            log.error("Error processing shipment.allocated event: {}", ex.getMessage(), ex);
        }
    }
}
