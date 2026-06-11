package projectlx.billing.payments.business.validator.api;

import projectlx.billing.payments.utils.requests.CreateWalletDepositRequest;
import projectlx.billing.payments.utils.requests.RecordPlatformUsageChargeRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationBillingSettingRequest;
import projectlx.billing.payments.utils.requests.SavePlatformActionChargeRequest;
import projectlx.billing.payments.utils.requests.SaveSubscriptionPackageRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface PlatformWalletBillingServiceValidator {
    ValidatorDto isSaveBillingSettingRequestValid(SaveOrganizationBillingSettingRequest request, Locale locale);
    ValidatorDto isCreateWalletDepositRequestValid(CreateWalletDepositRequest request, Locale locale);
    ValidatorDto isSaveActionChargeRequestValid(SavePlatformActionChargeRequest request, Locale locale);
    ValidatorDto isSaveSubscriptionPackageRequestValid(SaveSubscriptionPackageRequest request, Locale locale);
    ValidatorDto isRecordUsageChargeRequestValid(RecordPlatformUsageChargeRequest request, Locale locale);
}
