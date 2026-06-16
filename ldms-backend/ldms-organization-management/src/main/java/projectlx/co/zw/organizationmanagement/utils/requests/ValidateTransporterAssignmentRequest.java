package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ValidateTransporterAssignmentRequest {
    private Long shipperOrganizationId;
    private Long transportCompanyOrganizationId;
}
