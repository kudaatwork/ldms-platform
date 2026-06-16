package projectlx.trip.tracking.utils.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ldms.iot")
public class IotIntegrationProperties {

    /** When true, subscribes to MQTT GPS topics from onboard telematics units. */
    private boolean mqttEnabled = false;

    private String mqttBrokerUrl = "tcp://localhost:1884";

    private String mqttClientId = "ldms-trip-tracking";

    /** Contract topic pattern: ldms/iot/{organizationId}/{fleetAssetId}/gps */
    private String mqttGpsTopicPattern = "ldms/iot/+/+/gps";

    /** Outbound telemetry mirror for third-party dashboards. */
    private String mqttPublishTopicPrefix = "ldms/telemetry/trip";

    /** Auto-start corridor simulation when a trip enters IN_TRANSIT (demo / dev). */
    private boolean autoStartDemoSimulation = true;

    private int simulationTickSeconds = 2;

    private long simulationTickMs = 2000;

    private BigDecimalProperty simulationSpeedKmh = new BigDecimalProperty("72");

    @Getter
    @Setter
    public static class BigDecimalProperty {
        private String value = "72";

        public BigDecimalProperty() {
        }

        public BigDecimalProperty(String value) {
            this.value = value;
        }

        public java.math.BigDecimal asBigDecimal() {
            return new java.math.BigDecimal(value);
        }
    }
}
