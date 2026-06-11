package projectlx.shipment.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AllocateShipmentRequest {
    private Long shipmentId;
    private Long fleetDriverId;
    private Long fleetAssetId;
}
