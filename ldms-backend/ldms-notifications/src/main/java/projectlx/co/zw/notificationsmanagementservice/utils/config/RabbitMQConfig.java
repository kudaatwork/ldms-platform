package projectlx.co.zw.notificationsmanagementservice.utils.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue; // Correct import
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${notifications.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${notifications.rabbitmq.queue}")
    private String queueName;

    @Value("${notifications.rabbitmq.routing-key}")
    private String routingKey;

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue queue() {
        // This creates a durable queue with a dead-letter exchange
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", "") // Default exchange
                .withArgument("x-dead-letter-routing-key", queueName + ".dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        // Queue to hold messages that failed processing
        return QueueBuilder.durable(queueName + ".dlq").build();
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}