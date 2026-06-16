package projectlx.fuel.expenses.utils.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ producer topology for ldms-fuel-expenses.
 *
 * Publishes:
 *  - fuel.exchange / fuel.level_updated      — broadcast after every location update
 *  - fuel.exchange / fund_request.created    — driver raises a fund request
 *  - fuel.exchange / fund_request.approved   — manager approves; carries approvedLiters / approvedAmount
 *  - fuel.exchange / fund_request.rejected   — manager rejects with reason
 */
@Configuration
public class RabbitMQProducerConfig {

    public static final String FUEL_EXCHANGE                       = "fuel.exchange";
    public static final String ROUTING_KEY_FUEL_LEVEL_UPDATED      = "fuel.level_updated";
    public static final String ROUTING_KEY_FUND_REQUEST_CREATED    = "fund_request.created";
    public static final String ROUTING_KEY_FUND_REQUEST_APPROVED   = "fund_request.approved";
    public static final String ROUTING_KEY_FUND_REQUEST_REJECTED   = "fund_request.rejected";

    @Bean
    public TopicExchange fuelExchange() {
        return new TopicExchange(FUEL_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
