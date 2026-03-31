package projectlx.co.zw.audittrail.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditRabbitConfig {

    @Bean
    public TopicExchange auditExchange() {
        return ExchangeBuilder.topicExchange("ldms.audit.exchange").durable(true).build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable("ldms.audit.queue")
                .withArgument("x-message-ttl", 604800000L)
                .withArgument("x-dead-letter-exchange", "ldms.audit.dlx")
                .withArgument("x-dead-letter-routing-key", "audit.dead")
                .build();
    }

    @Bean
    public Binding auditBinding() {
        return BindingBuilder.bind(auditQueue()).to(auditExchange()).with("audit.#");
    }

    @Bean
    public TopicExchange auditDlx() {
        return ExchangeBuilder.topicExchange("ldms.audit.dlx").durable(true).build();
    }

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable("ldms.audit.dlq").build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(auditDlq()).to(auditDlx()).with("audit.dead");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jsonMessageConverter);
        return t;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
