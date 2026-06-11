package projectlx.inventory.management.utils.responses;

import lombok.Getter;
import lombok.Setter;
import projectlx.inventory.management.utils.dtos.ProcurementApprovalPolicyDto;

@Getter
@Setter
public class ProcurementSettingsResponse {
    private int statusCode;
    private boolean success;
    private String message;
    private ProcurementApprovalPolicyDto procurementApprovalPolicyDto;
}
