package projectlx.co.zw.organizationmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrganizationKycStagesRequest {

    /** When null, the organisation inherits the platform default. */
    private Integer kycRequiredApprovalStages;
}
