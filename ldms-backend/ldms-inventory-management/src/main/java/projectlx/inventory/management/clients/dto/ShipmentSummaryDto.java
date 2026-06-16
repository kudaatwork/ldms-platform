package projectlx.inventory.management.clients.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ShipmentSummaryDto {

    private Long id;
    private String sourceType;
    private Long inventoryTransferId;
    private Long salesOrderId;
    private Long fleetDriverId;
    private Long fleetAssetId;
    private Long tripId;
    private String status;
}
