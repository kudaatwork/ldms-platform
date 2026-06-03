package projectlx.co.zw.organizationmanagement.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycApprovalPolicyDto {

    private int defaultRequiredApprovalStages;
    private int minAllowedStages;
    private int maxAllowedStages;
}
