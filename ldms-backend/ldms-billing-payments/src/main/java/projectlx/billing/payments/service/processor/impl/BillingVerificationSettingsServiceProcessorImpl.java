package projectlx.billing.payments.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.logic.api.BillingVerificationSettingsService;
import projectlx.billing.payments.service.processor.api.BillingVerificationSettingsServiceProcessor;
import projectlx.billing.payments.utils.requests.UpdateBillingVerificationPolicyRequest;
import projectlx.billing.payments.utils.responses.BillingVerificationSettingsResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class BillingVerificationSettingsServiceProcessorImpl implements BillingVerificationSettingsServiceProcessor {

    private final BillingVerificationSettingsService billingVerificationSettingsService;

    @Override
    public BillingVerificationSettingsResponse getVerificationPolicy(Locale locale, String username) {
        return billingVerificationSettingsService.getVerificationPolicy(locale, username);
    }

    @Override
    public BillingVerificationSettingsResponse updateVerificationPolicy(
            UpdateBillingVerificationPolicyRequest request, Locale locale, String username) {
        return billingVerificationSettingsService.updateVerificationPolicy(request, locale, username);
    }
}
