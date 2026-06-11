package projectlx.inventory.management.business.logic.api;

public interface OutboxService {
    /**
     * Persist an outbox event within the current transaction boundary.
     *
     * @param aggregateType e.g., "SalesOrder"
     * @param aggregateId aggregate identifier as string (e.g., id or number)
     * @param eventType semantic event type, e.g., "SALES_ORDER_CREATED"
     * @param exchange target RabbitMQ exchange
     * @param routingKey target RabbitMQ routing key
     * @param payload arbitrary object that will be serialized into JSON
     */
    void writeEvent(String aggregateType,
                    String aggregateId,
                    String eventType,
                    String exchange,
                    String routingKey,
                    Object payload);
}
