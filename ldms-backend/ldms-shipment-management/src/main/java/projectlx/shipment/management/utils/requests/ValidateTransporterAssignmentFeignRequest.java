package projectlx.shipment.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ValidateTransporterAssignmentFeignRequest {
    private Long shipperOrganizationId;
    private Long transportCompanyOrganizationId;
}
