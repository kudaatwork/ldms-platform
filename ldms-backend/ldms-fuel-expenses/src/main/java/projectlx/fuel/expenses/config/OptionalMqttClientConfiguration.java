package projectlx.fuel.expenses.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Provides an empty Optional{@literal <}MqttClient{@literal >} bean when MQTT is disabled.
 * This keeps any beans that depend on {@code Optional<MqttClient>} satisfied without
 * requiring a live broker connection.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.iot.mqtt-enabled", havingValue = "false", matchIfMissing = true)
public class OptionalMqttClientConfiguration {

    @Bean
    public Optional<MqttClient> optionalFuelExpensesMqttClient() {
        return Optional.empty();
    }
}
