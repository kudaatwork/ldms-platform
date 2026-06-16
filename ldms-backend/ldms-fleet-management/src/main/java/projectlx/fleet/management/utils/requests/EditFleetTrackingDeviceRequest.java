package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EditFleetTrackingDeviceRequest {

    private Long id;
    private String deviceLabel;
    private String integrationProvider;

    private Long fleetAssetId;
    private Long fleetDriverId;
    private Long linkedUserId;

    private String deviceSerial;
    private String externalDeviceId;

    private Boolean tracksGps;
    private Boolean tracksFuel;
    private String notes;
}
