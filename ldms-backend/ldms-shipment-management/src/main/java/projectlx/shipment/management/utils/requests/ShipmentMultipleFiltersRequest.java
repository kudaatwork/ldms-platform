package projectlx.shipment.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ShipmentMultipleFiltersRequest {
    private Long organizationId;
    private String status;
    private Long inventoryTransferId;
    private String search;
}
