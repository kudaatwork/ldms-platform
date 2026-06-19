package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.RouteStopType;

@Getter
@Setter
@ToString
public class RouteStopRequest {

    private Integer stopSequence;
    private RouteStopType stopType;
    private Long warehouseLocationId;
    private Long branchId;
    private String locationLabel;
}
