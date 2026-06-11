package projectlx.inventory.management.clients;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.inventory.management.clients.dto.BillingCurrencyContextResponse;
import projectlx.inventory.management.clients.dto.BillingCurrencyConversionResponse;
import projectlx.inventory.management.clients.dto.LockCurrencyConversionRequest;
import projectlx.inventory.management.clients.dto.RecordPlatformUsageChargeRequest;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

public interface BillingPaymentsServiceClient {

    @GetMapping("/ldms-billing-payments/v1/system/billing/organization/{organizationId}/currency-context")
    BillingCurrencyContextResponse getOrganizationCurrencyContext(
            @PathVariable("organizationId") Long organizationId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-billing-payments/v1/system/billing/convert-and-lock")
    BillingCurrencyConversionResponse lockConversionForOrganization(
            @RequestBody LockCurrencyConversionRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @PostMapping("/ldms-billing-payments/v1/system/platform-wallet/usage/charge")
    void recordUsageCharge(
            @RequestBody RecordPlatformUsageChargeRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
