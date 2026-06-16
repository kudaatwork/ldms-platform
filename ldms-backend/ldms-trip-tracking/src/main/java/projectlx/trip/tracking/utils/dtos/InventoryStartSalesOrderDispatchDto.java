package projectlx.trip.tracking.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryStartSalesOrderDispatchDto {

    private Long salesOrderId;
    private Long startedByUserId;
    private Long tripId;
    private Long shipmentId;
}
