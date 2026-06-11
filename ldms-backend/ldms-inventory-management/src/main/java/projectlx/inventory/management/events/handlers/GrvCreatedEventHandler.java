package projectlx.inventory.management.events.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.inventory.management.business.logic.api.SalesOrderStatusManager;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.inventory.management.repository.SalesOrderRepository;
import projectlx.inventory.management.utils.config.RabbitMQConsumerConfig;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GrvCreatedEventHandler {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderStatusManager salesOrderStatusManager;

    @RabbitListener(queues = RabbitMQConsumerConfig.GRV_CREATED_QUEUE)
    public void handleGrvCreatedEvent(Map<String, Object> event) {
        if (event == null || !event.containsKey("salesOrderId")) {
            log.debug("GRV created event missing salesOrderId; ignoring.");
            return;
        }

        Long salesOrderId = parseLong(event.get("salesOrderId"));
        Long organizationId = parseLong(event.get("organizationId"));

        if (salesOrderId == null) {
            log.debug("GRV created event salesOrderId not parseable; ignoring.");
            return;
        }

        Optional<SalesOrder> salesOrderOpt = salesOrderRepository
                .findByIdAndEntityStatusNot(salesOrderId, EntityStatus.DELETED);

        if (salesOrderOpt.isEmpty()) {
            log.warn("GRV created event references missing SalesOrder id={}", salesOrderId);
            return;
        }

        SalesOrder salesOrder = salesOrderOpt.get();

        if (organizationId != null && !organizationId.equals(salesOrder.getSupplierOrganizationId())) {
            log.warn("GRV created event org mismatch: soId={} eventOrg={} soOrg={}",
                    salesOrderId, organizationId, salesOrder.getSupplierOrganizationId());
            return;
        }

        if (salesOrder.getStatus() == SalesOrderStatus.DELIVERED
                || salesOrder.getStatus() == SalesOrderStatus.FULFILLED) {
            log.debug("SalesOrder {} already delivered/fulfilled; ignoring GRV event.", salesOrderId);
            return;
        }

        if (!salesOrderStatusManager.canTransition(salesOrder.getStatus(), SalesOrderStatus.DELIVERED)) {
            log.warn("Cannot transition SalesOrder {} from {} to DELIVERED",
                    salesOrderId, salesOrder.getStatus());
            return;
        }

        salesOrderStatusManager.transition(
                salesOrder,
                SalesOrderStatus.DELIVERED,
                salesOrder.getFulfillmentWarehouseId(),
                "SYSTEM",
                Locale.ENGLISH);

        log.info("SalesOrder {} transitioned to DELIVERED from GRV created event", salesOrderId);
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
