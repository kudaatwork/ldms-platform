package projectlx.trip.tracking.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InventoryStartTransitDto {

    private Long transferId;
    private Long startedByUserId;
    private Long tripId;
    private Long shipmentId;
}
