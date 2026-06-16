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
 * Listens to sales.order.approved and auto-creates a shipment for bought-goods delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SalesOrderApprovedShipmentHandler {

    private final ShipmentService shipmentService;

    @RabbitListener(queues = RabbitMQConsumerConfig.SALES_ORDER_APPROVED_QUEUE)
    public void handleSalesOrderApprovedEvent(Map<String, Object> event) {
        if (event == null || !event.containsKey("salesOrderId")) {
            log.debug("sales.order.approved event missing salesOrderId; ignoring for shipment.");
            return;
        }
        log.info("Received sales.order.approved event for salesOrderId={}", event.get("salesOrderId"));
        try {
            shipmentService.createFromSalesOrderApprovedEvent(event, Locale.ENGLISH);
        } catch (Exception ex) {
            log.error("Failed to create shipment from sales order approved event salesOrderId={}: {}",
                    event.get("salesOrderId"), ex.getMessage(), ex);
            throw ex;
        }
    }
}
