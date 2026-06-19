package projectlx.fleet.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FleetTrackingIntegrationCredentialDto {

    private Long id;
    private Long organizationId;
    private String credentialLabel;
    private String ingestKey;
    private String integrationProvider;
    private String status;
    private Long fleetAssetId;
    private String vehicleRegistration;
    private String vehicleMakeModel;
    private String externalDeviceId;
    private String mqttTopic;
    private LocalDateTime lastTelemetryAt;
    private LocalDateTime createdAt;
}
