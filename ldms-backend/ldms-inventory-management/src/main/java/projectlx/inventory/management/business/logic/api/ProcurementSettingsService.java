package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.utils.requests.UpdateProcurementApprovalPolicyRequest;
import projectlx.inventory.management.utils.responses.ProcurementSettingsResponse;

import java.util.Locale;

public interface ProcurementSettingsService {

    ProcurementSettingsResponse getApprovalPolicy(Locale locale);

    ProcurementSettingsResponse updateApprovalPolicy(UpdateProcurementApprovalPolicyRequest request,
                                                     Locale locale,
                                                     String username);
}
