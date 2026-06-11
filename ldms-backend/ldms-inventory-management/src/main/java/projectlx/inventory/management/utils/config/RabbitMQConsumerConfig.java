package projectlx.inventory.management.utils.config;

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
    public static final String GRV_CREATED_QUEUE = "inventory.grv.created.queue";
    public static final String GRV_CREATED_ROUTING_KEY = "grv.created";
    public static final String TRANSFER_APPROVED_ROUTING_KEY = "inventory.transfer.approved";

    public static final String DLX_EXCHANGE = "inventory.dlx.exchange";
    public static final String DLQ_SUFFIX = ".dlq";

    // Billing payment verified queue - receives payment confirmation from billing service
    public static final String BILLING_EXCHANGE = "billing.exchange";
    public static final String BILLING_PAYMENT_VERIFIED_QUEUE = "inventory.billing.payment.verified.queue";
    public static final String BILLING_PAYMENT_VERIFIED_ROUTING_KEY = "billing.payment.verified";

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(INVENTORY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue grvCreatedQueue() {
        return QueueBuilder.durable(GRV_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", GRV_CREATED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue grvCreatedDLQ() {
        return QueueBuilder.durable(GRV_CREATED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding grvCreatedBinding() {
        return BindingBuilder.bind(grvCreatedQueue())
                .to(inventoryExchange())
                .with(GRV_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding grvCreatedDLQBinding() {
        return BindingBuilder.bind(grvCreatedDLQ())
                .to(deadLetterExchange())
                .with(GRV_CREATED_QUEUE + DLQ_SUFFIX);
    }

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange(BILLING_EXCHANGE, true, false);
    }

    @Bean
    public Queue billingPaymentVerifiedQueue() {
        return QueueBuilder.durable(BILLING_PAYMENT_VERIFIED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BILLING_PAYMENT_VERIFIED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue billingPaymentVerifiedDLQ() {
        return QueueBuilder.durable(BILLING_PAYMENT_VERIFIED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding billingPaymentVerifiedBinding() {
        return BindingBuilder.bind(billingPaymentVerifiedQueue())
                .to(billingExchange())
                .with(BILLING_PAYMENT_VERIFIED_ROUTING_KEY);
    }

    @Bean
    public Binding billingPaymentVerifiedDLQBinding() {
        return BindingBuilder.bind(billingPaymentVerifiedDLQ())
                .to(deadLetterExchange())
                .with(BILLING_PAYMENT_VERIFIED_QUEUE + DLQ_SUFFIX);
    }

    // Reuse JSON message converter from RabbitMQProducerConfig
}
