package projectlx.fuel.expenses.utils.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ldms.iot")
public class IotIntegrationProperties {

    /** When true, subscribes to MQTT fuel-level topics from onboard telematics units. */
    private boolean mqttEnabled = false;

    private String mqttBrokerUrl = "tcp://localhost:1884";

    private String mqttClientId = "ldms-fuel-expenses";

    /** Contract topic pattern: ldms/iot/fuel/{fleetAssetId}/level */
    private String mqttFuelTopicPattern = "ldms/iot/fuel/+/level";

    /** Outbound mirror topic prefix for third-party dashboards. */
    private String mqttPublishTopicPrefix = "ldms/telemetry/fuel";
}
