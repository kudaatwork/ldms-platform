package projectlx.co.zw.organizationmanagement.utils.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures the notifications direct exchange and queue exist before organisation-management
 * publishes KYC/registration emails. Without a bound queue, RabbitMQ silently drops messages.
 */
@Configuration
public class NotificationsRabbitTopologyConfig {

    @Value("${ldms.notifications.rabbitmq.exchange:notifications.direct}")
    private String exchangeName;

    @Value("${ldms.notifications.rabbitmq.queue:notifications.queue}")
    private String queueName;

    @Value("${ldms.notifications.rabbitmq.routing-key:notifications.send}")
    private String routingKey;

    @Bean
    public DirectExchange notificationsDirectExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", queueName + ".dlq")
                .build();
    }

    @Bean
    public Queue notificationsDeadLetterQueue() {
        return QueueBuilder.durable(queueName + ".dlq").build();
    }

    @Bean
    public Binding notificationsQueueBinding(
            Queue notificationsQueue, DirectExchange notificationsDirectExchange, RabbitAdmin rabbitAdmin) {
        Binding binding = BindingBuilder.bind(notificationsQueue)
                .to(notificationsDirectExchange)
                .with(routingKey);
        rabbitAdmin.declareExchange(notificationsDirectExchange);
        rabbitAdmin.declareQueue(notificationsQueue);
        rabbitAdmin.declareBinding(binding);
        return binding;
    }
}
