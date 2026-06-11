package projectlx.trip.tracking.utils.config;

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

    public static final String SHIPMENT_EXCHANGE = "shipment.exchange";
    public static final String SHIPMENT_ALLOCATED_QUEUE = "trip.shipment.allocated.queue";
    public static final String SHIPMENT_ALLOCATED_ROUTING_KEY = "shipment.allocated";
    public static final String DLX_EXCHANGE = "trip.dlx.exchange";
    public static final String DLQ_SUFFIX = ".dlq";

    @Bean
    public TopicExchange shipmentExchange() {
        return new TopicExchange(SHIPMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange tripDeadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // === SHIPMENT ALLOCATED CONSUMER ===

    @Bean
    public Queue tripShipmentAllocatedQueue() {
        return QueueBuilder.durable(SHIPMENT_ALLOCATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SHIPMENT_ALLOCATED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue tripShipmentAllocatedDlq() {
        return QueueBuilder.durable(SHIPMENT_ALLOCATED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding tripShipmentAllocatedBinding() {
        return BindingBuilder.bind(tripShipmentAllocatedQueue())
                .to(shipmentExchange())
                .with(SHIPMENT_ALLOCATED_ROUTING_KEY);
    }

    @Bean
    public Binding tripShipmentAllocatedDlqBinding() {
        return BindingBuilder.bind(tripShipmentAllocatedDlq())
                .to(tripDeadLetterExchange())
                .with(SHIPMENT_ALLOCATED_QUEUE + DLQ_SUFFIX);
    }
}
