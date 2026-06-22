package projectlx.co.zw.shared_library.billing;

import java.util.Locale;

@FunctionalInterface
public interface PlatformWalletBillingClient {
    PlatformWalletChargeResponse recordUsageCharge(RecordPlatformUsageChargeRequest request, Locale locale);
}
