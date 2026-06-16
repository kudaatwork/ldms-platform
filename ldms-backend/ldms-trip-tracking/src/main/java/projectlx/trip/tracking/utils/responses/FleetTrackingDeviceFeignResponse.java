package projectlx.trip.tracking.utils.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FleetTrackingDeviceFeignResponse {

    private Long id;
    private Long organizationId;
    private Long fleetAssetId;
    private Long fleetDriverId;
    private Long linkedUserId;

    private String deviceType;
    private String installStatus;
    private String integrationProvider;
    private String deviceLabel;
    private String ingestKey;

    private boolean tracksGps;
    private boolean tracksFuel;
    private String mqttTopic;

    private LocalDateTime installedAt;
    private LocalDateTime lastTelemetryAt;
    private String entityStatus;
}
