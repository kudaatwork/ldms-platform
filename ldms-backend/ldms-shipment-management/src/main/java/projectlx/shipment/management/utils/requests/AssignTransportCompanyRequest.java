package projectlx.shipment.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AssignTransportCompanyRequest {

    private Long shipmentId;
    private Long transportCompanyOrganizationId;
}
