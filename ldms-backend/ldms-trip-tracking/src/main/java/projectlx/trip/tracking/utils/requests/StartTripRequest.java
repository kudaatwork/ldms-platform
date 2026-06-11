package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StartTripRequest {

    private Long shipmentId;
    private Long fleetDriverId;
    private Long fleetAssetId;
    private Long startedByUserId;
}
