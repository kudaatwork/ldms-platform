package projectlx.inventory.management.utils.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProcurementApprovalPolicyRequest {
    private Integer defaultRequiredApprovalStages;
    private Integer organizationRequiredApprovalStages;
    private Long organizationId;
}
