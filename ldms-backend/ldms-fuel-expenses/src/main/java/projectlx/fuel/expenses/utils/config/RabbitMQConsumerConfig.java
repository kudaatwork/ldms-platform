package projectlx.fuel.expenses.utils.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ consumer topology for ldms-fuel-expenses.
 *
 * Consumes:
 *  - trip.exchange / trip.started          → fuel.trip.started.queue
 *  - trip.exchange / trip.location_updated → fuel.trip.location_updated.queue
 *
 * Both queues are backed by a dedicated dead-letter exchange/queue pair.
 */
@Configuration
@EnableRabbit
public class RabbitMQConsumerConfig {

    // ── Source exchange (declared here; trip-tracking owns the authoritative declaration) ──
    public static final String TRIP_EXCHANGE = "trip.exchange";

    // ── Consumed routing keys ──
    public static final String ROUTING_KEY_TRIP_STARTED          = "trip.started";
    public static final String ROUTING_KEY_TRIP_LOCATION_UPDATED = "trip.location_updated";

    // ── Consumer queues ──
    public static final String FUEL_TRIP_STARTED_QUEUE          = "fuel.trip.started.queue";
    public static final String FUEL_TRIP_LOCATION_UPDATED_QUEUE = "fuel.trip.location_updated.queue";

    // ── Dead-letter infrastructure ──
    public static final String DLX_EXCHANGE = "fuel.dlx.exchange";
    public static final String DLQ_SUFFIX   = ".dlq";

    @Bean
    public TopicExchange tripExchangeConsumer() {
        return new TopicExchange(TRIP_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange fuelDeadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // ── trip.started queue + DLQ ──

    @Bean
    public Queue fuelTripStartedQueue() {
        return QueueBuilder.durable(FUEL_TRIP_STARTED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FUEL_TRIP_STARTED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue fuelTripStartedDlq() {
        return QueueBuilder.durable(FUEL_TRIP_STARTED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding fuelTripStartedBinding() {
        return BindingBuilder.bind(fuelTripStartedQueue())
                .to(tripExchangeConsumer())
                .with(ROUTING_KEY_TRIP_STARTED);
    }

    @Bean
    public Binding fuelTripStartedDlqBinding() {
        return BindingBuilder.bind(fuelTripStartedDlq())
                .to(fuelDeadLetterExchange())
                .with(FUEL_TRIP_STARTED_QUEUE + DLQ_SUFFIX);
    }

    // ── trip.location_updated queue + DLQ ──

    @Bean
    public Queue fuelTripLocationUpdatedQueue() {
        return QueueBuilder.durable(FUEL_TRIP_LOCATION_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FUEL_TRIP_LOCATION_UPDATED_QUEUE + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue fuelTripLocationUpdatedDlq() {
        return QueueBuilder.durable(FUEL_TRIP_LOCATION_UPDATED_QUEUE + DLQ_SUFFIX).build();
    }

    @Bean
    public Binding fuelTripLocationUpdatedBinding() {
        return BindingBuilder.bind(fuelTripLocationUpdatedQueue())
                .to(tripExchangeConsumer())
                .with(ROUTING_KEY_TRIP_LOCATION_UPDATED);
    }

    @Bean
    public Binding fuelTripLocationUpdatedDlqBinding() {
        return BindingBuilder.bind(fuelTripLocationUpdatedDlq())
                .to(fuelDeadLetterExchange())
                .with(FUEL_TRIP_LOCATION_UPDATED_QUEUE + DLQ_SUFFIX);
    }
}
