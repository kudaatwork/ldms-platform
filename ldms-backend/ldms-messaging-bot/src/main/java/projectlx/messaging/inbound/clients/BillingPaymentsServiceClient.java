package projectlx.messaging.inbound.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import projectlx.co.zw.shared_library.billing.PlatformWalletChargeResponse;
import projectlx.co.zw.shared_library.billing.RecordPlatformUsageChargeRequest;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

public interface BillingPaymentsServiceClient {

    @PostMapping("/ldms-billing-payments/v1/system/platform-wallet/usage/charge")
    PlatformWalletChargeResponse recordUsageCharge(
            @RequestBody RecordPlatformUsageChargeRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @GetMapping("/ldms-billing-payments/v1/system/platform-wallet/organization/{organizationId}/summary")
    JsonNode getWalletSummary(
            @PathVariable("organizationId") Long organizationId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);

    @GetMapping("/ldms-billing-payments/v1/system/platform-wallet/pricing-catalog")
    JsonNode getPublicPricingCatalog(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale);
}
