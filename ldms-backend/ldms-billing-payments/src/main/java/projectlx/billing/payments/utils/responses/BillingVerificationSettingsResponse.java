package projectlx.billing.payments.utils.responses;

import lombok.Getter;
import lombok.Setter;
import projectlx.billing.payments.utils.dtos.BillingVerificationPolicyDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@Getter
@Setter
public class BillingVerificationSettingsResponse extends CommonResponse {
    private BillingVerificationPolicyDto billingVerificationPolicyDto;
}
