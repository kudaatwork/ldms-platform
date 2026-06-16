package projectlx.trip.tracking.business.logic.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.utils.config.IotIntegrationProperties;
import projectlx.trip.tracking.utils.config.RabbitMQProducerConfig;
import projectlx.trip.tracking.utils.dtos.TripLiveSnapshotDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Publishes live GPS telemetry to SSE subscribers, RabbitMQ (fuel-expenses), and optional MQTT mirror.
 */
@RequiredArgsConstructor
@Slf4j
public class TripTelemetryPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final TripLiveSseRegistry sseRegistry;
    private final IotIntegrationProperties iotProperties;
    private final ObjectMapper objectMapper;
    private final Optional<MqttClient> mqttClient;

    public void publish(Trip trip, TripLiveSnapshotDto snapshot) {
        snapshot.setRecordedAt(LocalDateTime.now());
        sseRegistry.broadcast(trip.getId(), snapshot);
        publishRabbit(trip, snapshot);
        publishMqtt(trip, snapshot);
    }

    private void publishRabbit(Trip trip, TripLiveSnapshotDto snapshot) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("tripId", trip.getId());
            payload.put("tripNumber", trip.getTripNumber());
            payload.put("organizationId", trip.getOrganizationId());
            payload.put("fleetAssetId", trip.getFleetAssetId());
            payload.put("fleetDriverId", trip.getFleetDriverId());
            payload.put("shipmentId", trip.getShipmentId());
            payload.put("latitude", snapshot.getLatitude());
            payload.put("longitude", snapshot.getLongitude());
            payload.put("speedKmh", snapshot.getSpeedKmh());
            payload.put("headingDeg", snapshot.getHeadingDeg());
            payload.put("overallProgressPct", snapshot.getOverallProgressPct());
            payload.put("moving", snapshot.isMoving());
            payload.put("recordedAt", snapshot.getRecordedAt() != null ? snapshot.getRecordedAt().toString() : null);
            rabbitTemplate.convertAndSend(
                    RabbitMQProducerConfig.TRIP_EXCHANGE,
                    RabbitMQProducerConfig.ROUTING_KEY_TRIP_LOCATION_UPDATED,
                    payload);
        } catch (Exception ex) {
            log.error("Failed to publish trip.location_updated for trip {}: {}", trip.getId(), ex.getMessage());
        }
    }

    private void publishMqtt(Trip trip, TripLiveSnapshotDto snapshot) {
        if (!iotProperties.isMqttEnabled() || mqttClient.isEmpty() || !mqttClient.get().isConnected()) {
            return;
        }
        try {
            String topic = iotProperties.getMqttPublishTopicPrefix() + "/" + trip.getId() + "/location";
            mqttClient.get().publish(topic, objectMapper.writeValueAsBytes(snapshot), 1, false);
        } catch (Exception ex) {
            log.debug("MQTT publish skipped for trip {}: {}", trip.getId(), ex.getMessage());
        }
    }

    public TripLiveSnapshotDto buildSnapshot(Trip trip,
                                             BigDecimal lat,
                                             BigDecimal lng,
                                             BigDecimal speedKmh,
                                             BigDecimal headingDeg,
                                             BigDecimal progressPct,
                                             boolean simulationActive,
                                             boolean moving) {
        TripLiveSnapshotDto dto = new TripLiveSnapshotDto();
        dto.setTripId(trip.getId());
        dto.setTripNumber(trip.getTripNumber());
        dto.setStatus(trip.getStatus() != null ? trip.getStatus().name() : null);
        dto.setFromWarehouseName(trip.getFromWarehouseName());
        dto.setToWarehouseName(trip.getToWarehouseName());
        dto.setLatitude(lat);
        dto.setLongitude(lng);
        dto.setSpeedKmh(speedKmh);
        dto.setHeadingDeg(headingDeg);
        dto.setOverallProgressPct(progressPct);
        dto.setSimulationActive(simulationActive);
        dto.setMoving(moving);
        return dto;
    }
}
