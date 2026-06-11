package projectlx.inventory.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcurementApprovalPolicyDto {
    private int defaultRequiredApprovalStages;
    private int minAllowedStages;
    private int maxAllowedStages;
}
