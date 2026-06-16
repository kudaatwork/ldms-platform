package projectlx.trip.tracking.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@ConditionalOnProperty(name = "ldms.iot.mqtt-enabled", havingValue = "false", matchIfMissing = true)
public class OptionalMqttClientConfiguration {

    @Bean
    public Optional<MqttClient> optionalTripTrackingMqttClient() {
        return Optional.empty();
    }
}
