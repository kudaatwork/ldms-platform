package projectlx.billing.payments.utils.config;

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
    public static final String GRV_CREATED_QUEUE = "billing.grv.created.queue";
    public static final String GRV_CREATED_ROUTING_KEY = "grv.created";
    public static final String PO_APPROVED_QUEUE = "billing.po.approved.queue";
    public static final String PO_APPROVED_ROUTING_KEY = "po.approved";
    public static final String DLX_EXCHANGE = "billing.dlx.exchange";
    public static final String DLQ_SUFFIX = ".dlq";

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(INVENTORY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange billingDeadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // === GRV CREATED CONSUMER ===

    @Bean
    public Queue billingGrvCreatedQueue() {
        return QueueBuilder.durable(GRV_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", GRV_CREATED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue billingGrvCreatedDlq() {
        return QueueBuilder.durable(GRV_CREATED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding billingGrvCreatedBinding() {
        return BindingBuilder.bind(billingGrvCreatedQueue())
                .to(inventoryExchange())
                .with(GRV_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding billingGrvCreatedDlqBinding() {
        return BindingBuilder.bind(billingGrvCreatedDlq())
                .to(billingDeadLetterExchange())
                .with(GRV_CREATED_QUEUE + DLQ_SUFFIX);
    }

    // === PO APPROVED CONSUMER ===

    @Bean
    public Queue billingPoApprovedQueue() {
        return QueueBuilder.durable(PO_APPROVED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PO_APPROVED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue billingPoApprovedDlq() {
        return QueueBuilder.durable(PO_APPROVED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding billingPoApprovedBinding() {
        return BindingBuilder.bind(billingPoApprovedQueue())
                .to(inventoryExchange())
                .with(PO_APPROVED_ROUTING_KEY);
    }

    @Bean
    public Binding billingPoApprovedDlqBinding() {
        return BindingBuilder.bind(billingPoApprovedDlq())
                .to(billingDeadLetterExchange())
                .with(PO_APPROVED_QUEUE + DLQ_SUFFIX);
    }
}
