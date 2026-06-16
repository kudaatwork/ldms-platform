package projectlx.shipment.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BorderClearanceMultipleFiltersRequest {

    private Long shipmentId;
    private String status;
    private Long organizationId;
}
