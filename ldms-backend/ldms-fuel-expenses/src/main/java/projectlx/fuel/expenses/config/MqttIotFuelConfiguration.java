package projectlx.fuel.expenses.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.fuel.expenses.utils.config.IotIntegrationProperties;
import projectlx.fuel.expenses.utils.config.RabbitMQProducerConfig;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MQTT inbound fuel-level integration — contract-ready for onboard telematics hardware.
 *
 * Disabled by default ({@code ldms.iot.mqtt-enabled=false}) until Mosquitto broker
 * credentials and device topics are provisioned.
 *
 * Topic contract: {@code ldms/iot/fuel/{fleetAssetId}/level}
 *
 * Expected JSON payload:
 * <pre>
 * {
 *   "tripId": 123,
 *   "fleetAssetId": 456,
 *   "organizationId": 789,
 *   "fuelRemainingLiters": 312.50,
 *   "fuelLevelPct": 78.13
 * }
 * </pre>
 * When enabled, the service mirrors the incoming event directly to
 * {@code fuel.exchange / fuel.level_updated} so downstream consumers
 * (billing, notifications) receive hardware-sourced readings in addition
 * to the computed readings from trip-tracking GPS events.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.iot.mqtt-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MqttIotFuelConfiguration {

    private final IotIntegrationProperties iotProperties;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Bean(destroyMethod = "disconnect")
    public MqttClient fuelExpensesMqttClient() throws MqttException {
        MqttClient client = new MqttClient(
                iotProperties.getMqttBrokerUrl(),
                iotProperties.getMqttClientId() + "-" + System.currentTimeMillis(),
                new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT fuel connection lost: {}", cause != null ? cause.getMessage() : "unknown");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                handleFuelLevelMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // outbound publish ack — not used for inbound consumer
            }
        });

        client.connect(options);
        client.subscribe(iotProperties.getMqttFuelTopicPattern(), 1);
        log.info("MQTT fuel IoT subscriber connected to {} on topic {}",
                iotProperties.getMqttBrokerUrl(), iotProperties.getMqttFuelTopicPattern());
        return client;
    }

    @Bean
    public Optional<MqttClient> optionalFuelExpensesMqttClient(MqttClient fuelExpensesMqttClient) {
        return Optional.of(fuelExpensesMqttClient);
    }

    private void handleFuelLevelMessage(String topic, MqttMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());

            Long tripId = readLong(node, "tripId");
            if (tripId == null) {
                log.debug("Ignoring MQTT fuel message without tripId on topic {}", topic);
                return;
            }

            // Mirror inbound hardware reading to fuel.exchange
            Map<String, Object> event = new HashMap<>();
            event.put("tripId",              tripId);
            event.put("fleetAssetId",        readLong(node, "fleetAssetId"));
            event.put("organizationId",      readLong(node, "organizationId"));
            event.put("fuelRemainingLiters", readDecimal(node, "fuelRemainingLiters"));
            event.put("fuelLevelPct",        readDecimal(node, "fuelLevelPct"));
            event.put("source",              "mqtt-iot");

            rabbitTemplate.convertAndSend(
                    RabbitMQProducerConfig.FUEL_EXCHANGE,
                    RabbitMQProducerConfig.ROUTING_KEY_FUEL_LEVEL_UPDATED,
                    event);

            log.info("Mirrored MQTT fuel.level_updated for tripId={} fuelPct={}",
                    tripId, event.get("fuelLevelPct"));

        } catch (Exception ex) {
            log.warn("Failed to process MQTT fuel message on {}: {}", topic, ex.getMessage());
        }
    }

    private static Long readLong(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asLong();
        }
        return null;
    }

    private static BigDecimal readDecimal(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return new BigDecimal(node.get(field).asText());
        }
        return null;
    }
}
