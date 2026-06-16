package projectlx.trip.tracking.config;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import projectlx.trip.tracking.business.logic.api.TripService;
import projectlx.trip.tracking.business.logic.api.TripTelemetryIngestService;
import projectlx.trip.tracking.utils.config.IotIntegrationProperties;
import projectlx.trip.tracking.utils.requests.RecordLocationRequest;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

/**
 * MQTT inbound GPS integration — contract-ready for onboard telematics hardware.
 * Disabled by default until Mosquitto broker credentials and device topics are provisioned.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.iot.mqtt-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MqttIotConfiguration {

    private final IotIntegrationProperties iotProperties;
    @Lazy
    private final TripService tripService;
    @Lazy
    private final TripTelemetryIngestService tripTelemetryIngestService;
    private final ObjectMapper objectMapper;

    // Topic pattern: ldms/iot/{orgId}/{assetId}/gps
    private static final String LDMS_TOPIC_PREFIX = "ldms/iot/";

    @Bean(destroyMethod = "disconnect")
    public MqttClient tripTrackingMqttClient() throws MqttException {
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
                log.warn("MQTT connection lost: {}", cause != null ? cause.getMessage() : "unknown");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                handleGpsMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // outbound publish ack
            }
        });

        client.connect(options);
        client.subscribe(iotProperties.getMqttGpsTopicPattern(), 1);
        log.info("MQTT IoT subscriber connected to {} on topic {}",
                iotProperties.getMqttBrokerUrl(), iotProperties.getMqttGpsTopicPattern());
        return client;
    }

    @Bean
    public Optional<MqttClient> optionalTripTrackingMqttClient(MqttClient tripTrackingMqttClient) {
        return Optional.of(tripTrackingMqttClient);
    }

    private void handleGpsMessage(String topic, MqttMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());

            // Attempt structured LDMS topic: ldms/iot/{orgId}/{assetId}/gps
            if (topic != null && topic.startsWith(LDMS_TOPIC_PREFIX) && topic.endsWith("/gps")) {
                String[] parts = topic.substring(LDMS_TOPIC_PREFIX.length()).split("/");
                if (parts.length == 3) {
                    Long orgId = parseLong(parts[0]);
                    Long assetId = parseLong(parts[1]);
                    if (orgId != null && assetId != null) {
                        BigDecimal lat = readDecimal(node, "latitude", "lat");
                        BigDecimal lng = readDecimal(node, "longitude", "lng", "lon");
                        BigDecimal speed = readDecimal(node, "speedKmh", "speed");
                        BigDecimal heading = readDecimal(node, "headingDeg", "heading");
                        tripTelemetryIngestService.ingestFromAsset(orgId, assetId, lat, lng, speed, heading);
                        return;
                    }
                }
            }

            // Legacy fallback — trip-id-based recording used before tracking devices
            Long tripId = readLong(node, "tripId");
            if (tripId == null) {
                log.debug("Ignoring MQTT GPS message: no recognized topic structure and no tripId on topic {}", topic);
                return;
            }
            RecordLocationRequest request = new RecordLocationRequest();
            request.setTripId(tripId);
            request.setLatitude(readDecimal(node, "latitude", "lat"));
            request.setLongitude(readDecimal(node, "longitude", "lng", "lon"));
            tripService.recordLocation(request, Locale.ENGLISH, "mqtt-iot-ingest");
        } catch (Exception ex) {
            log.warn("Failed to process MQTT GPS on {}: {}", topic, ex.getMessage());
        }
    }

    private static Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Long readLong(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asLong();
        }
        return null;
    }

    private static BigDecimal readDecimal(JsonNode node, String... fields) {
        for (String field : fields) {
            if (node.has(field) && !node.get(field).isNull()) {
                return new BigDecimal(node.get(field).asText());
            }
        }
        return null;
    }
}
