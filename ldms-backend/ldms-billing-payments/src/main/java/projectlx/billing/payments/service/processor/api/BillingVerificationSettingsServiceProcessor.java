package projectlx.billing.payments.service.processor.api;

import projectlx.billing.payments.utils.requests.UpdateBillingVerificationPolicyRequest;
import projectlx.billing.payments.utils.responses.BillingVerificationSettingsResponse;

import java.util.Locale;

public interface BillingVerificationSettingsServiceProcessor {

    BillingVerificationSettingsResponse getVerificationPolicy(Locale locale, String username);

    BillingVerificationSettingsResponse updateVerificationPolicy(
            UpdateBillingVerificationPolicyRequest request, Locale locale, String username);
}
