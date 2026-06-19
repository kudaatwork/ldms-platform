package projectlx.shipment.management.utils.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConsumerConfig {

    public static final String INVENTORY_EXCHANGE = "inventory.exchange";
    public static final String TRANSFER_APPROVED_QUEUE = "shipment.transfer.approved.queue";
    public static final String TRANSFER_APPROVED_ROUTING_KEY = "inventory.transfer.approved";
    public static final String SALES_ORDER_APPROVED_QUEUE = "shipment.sales.order.approved.queue";
    public static final String SALES_ORDER_APPROVED_ROUTING_KEY = "sales.order.approved";
    public static final String CROSS_DOCK_DISPATCH_CREATED_QUEUE = "shipment.cross.dock.dispatch.created.queue";
    public static final String CROSS_DOCK_DISPATCH_CREATED_ROUTING_KEY = "cross.dock.dispatch.created";
    public static final String DLX_EXCHANGE = "shipment.dlx.exchange";
    public static final String DLQ_SUFFIX = ".dlq";

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(INVENTORY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange shipmentDeadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // === TRANSFER APPROVED CONSUMER ===

    @Bean
    public Queue shipmentTransferApprovedQueue() {
        return QueueBuilder.durable(TRANSFER_APPROVED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRANSFER_APPROVED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue shipmentTransferApprovedDlq() {
        return QueueBuilder.durable(TRANSFER_APPROVED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding shipmentTransferApprovedBinding() {
        return BindingBuilder.bind(shipmentTransferApprovedQueue())
                .to(inventoryExchange())
                .with(TRANSFER_APPROVED_ROUTING_KEY);
    }

    @Bean
    public Binding shipmentTransferApprovedDlqBinding() {
        return BindingBuilder.bind(shipmentTransferApprovedDlq())
                .to(shipmentDeadLetterExchange())
                .with(TRANSFER_APPROVED_QUEUE + DLQ_SUFFIX);
    }

    // === SALES ORDER APPROVED CONSUMER ===

    @Bean
    public Queue shipmentSalesOrderApprovedQueue() {
        return QueueBuilder.durable(SALES_ORDER_APPROVED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SALES_ORDER_APPROVED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue shipmentSalesOrderApprovedDlq() {
        return QueueBuilder.durable(SALES_ORDER_APPROVED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding shipmentSalesOrderApprovedBinding() {
        return BindingBuilder.bind(shipmentSalesOrderApprovedQueue())
                .to(inventoryExchange())
                .with(SALES_ORDER_APPROVED_ROUTING_KEY);
    }

    @Bean
    public Binding shipmentSalesOrderApprovedDlqBinding() {
        return BindingBuilder.bind(shipmentSalesOrderApprovedDlq())
                .to(shipmentDeadLetterExchange())
                .with(SALES_ORDER_APPROVED_QUEUE + DLQ_SUFFIX);
    }

    // === CROSS-DOCK DISPATCH CREATED CONSUMER ===

    @Bean
    public Queue shipmentCrossDockDispatchCreatedQueue() {
        return QueueBuilder.durable(CROSS_DOCK_DISPATCH_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CROSS_DOCK_DISPATCH_CREATED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue shipmentCrossDockDispatchCreatedDlq() {
        return QueueBuilder.durable(CROSS_DOCK_DISPATCH_CREATED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding shipmentCrossDockDispatchCreatedBinding() {
        return BindingBuilder.bind(shipmentCrossDockDispatchCreatedQueue())
                .to(inventoryExchange())
                .with(CROSS_DOCK_DISPATCH_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding shipmentCrossDockDispatchCreatedDlqBinding() {
        return BindingBuilder.bind(shipmentCrossDockDispatchCreatedDlq())
                .to(shipmentDeadLetterExchange())
                .with(CROSS_DOCK_DISPATCH_CREATED_QUEUE + DLQ_SUFFIX);
    }
}
