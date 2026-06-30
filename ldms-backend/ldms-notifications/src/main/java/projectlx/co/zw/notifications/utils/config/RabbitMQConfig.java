package projectlx.co.zw.notifications.utils.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Value("${notifications.rabbitmq.platform-bell-queue}")
    private String platformBellQueueName;

    @Value("${notifications.rabbitmq.platform-bell-routing-key}")
    private String platformBellRoutingKey;

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setIgnoreDeclarationExceptions(true);
        return admin;
    }

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
    public Binding binding(@Qualifier("queue") Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    @Bean
    public Queue platformBellQueue() {
        return QueueBuilder.durable(platformBellQueueName)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", platformBellQueueName + ".dlq")
                .build();
    }

    @Bean
    public Queue platformBellDeadLetterQueue() {
        return QueueBuilder.durable(platformBellQueueName + ".dlq").build();
    }

    @Bean
    public Binding platformBellBinding(@Qualifier("platformBellQueue") Queue platformBellQueue, DirectExchange exchange) {
        return BindingBuilder.bind(platformBellQueue).to(exchange).with(platformBellRoutingKey);
    }
}