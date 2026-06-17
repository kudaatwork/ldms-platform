package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AssignFleetAssetDriverRequest {
    /** Fleet driver id to assign, or null / omitted to clear the assignment. */
    private Long fleetDriverId;
}
