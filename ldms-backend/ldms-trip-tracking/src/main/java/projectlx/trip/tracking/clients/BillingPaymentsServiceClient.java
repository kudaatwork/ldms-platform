package projectlx.trip.tracking.clients;

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
}
