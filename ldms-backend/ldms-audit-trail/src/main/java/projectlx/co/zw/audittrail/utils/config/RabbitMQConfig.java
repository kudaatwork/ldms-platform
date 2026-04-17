package projectlx.co.zw.audittrail.utils.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogPayload;

@Configuration
public class RabbitMQConfig {

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
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        // Map the producer's __TypeId__ header (AuditLogDto from shared library) to the
        // local AuditLogPayload, so deserialization is always correct regardless of
        // which Spring AMQP version the producer used to set the header.
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setIdClassMapping(Map.of(
                "projectlx.co.zw.shared_library.utils.dtos.AuditLogDto", AuditLogPayload.class
        ));
        typeMapper.setTrustedPackages(
                "projectlx.co.zw.audittrail",
                "projectlx.co.zw.shared_library",
                "java.util",
                "java.lang"
        );
        converter.setJavaTypeMapper(typeMapper);
        return converter;
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
