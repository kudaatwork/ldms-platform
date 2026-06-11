package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UpdateShipmentStatusFeignRequest {
    private Long shipmentId;
    private String status;
    private Long tripId;
    private String notes;
}
