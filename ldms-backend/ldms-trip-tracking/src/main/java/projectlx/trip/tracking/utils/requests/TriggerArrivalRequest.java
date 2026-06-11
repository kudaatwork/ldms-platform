package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TriggerArrivalRequest {

    private Long tripId;
    private Long driverUserId;
}
