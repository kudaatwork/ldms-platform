package projectlx.fleet.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FleetTrackingDeviceDto {

    private Long id;
    private Long organizationId;
    private Long fleetAssetId;
    private Long fleetDriverId;
    private Long linkedUserId;

    private String deviceType;
    private String installStatus;
    private String integrationProvider;
    private String deviceLabel;
    private String deviceSerial;
    private String externalDeviceId;
    private String ingestKey;

    private boolean tracksGps;
    private boolean tracksFuel;
    private String mqttTopic;

    // Denormalised from fleet_asset lookup on list
    private String vehicleRegistration;
    private String vehicleMakeModel;

    private LocalDateTime installedAt;
    private LocalDateTime lastTelemetryAt;
    private String notes;

    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
