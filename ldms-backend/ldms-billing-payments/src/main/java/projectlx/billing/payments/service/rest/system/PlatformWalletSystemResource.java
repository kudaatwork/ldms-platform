package projectlx.billing.payments.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.billing.payments.service.processor.api.PlatformWalletBillingServiceProcessor;
import projectlx.billing.payments.utils.requests.RecordPlatformUsageChargeRequest;
import projectlx.billing.payments.utils.responses.PlatformWalletResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-billing-payments/v1/system/platform-wallet")
@Tag(name = "Platform Wallet System Resource", description = "Inter-service usage billing and wallet deduction")
@RequiredArgsConstructor
public class PlatformWalletSystemResource {

    private final PlatformWalletBillingServiceProcessor platformWalletBillingServiceProcessor;

    @Auditable(action = "SYSTEM_GET_WALLET_SUMMARY")
    @GetMapping("/organization/{organizationId}/summary")
    @Operation(summary = "Wallet summary for an organisation (internal services)")
    public ResponseEntity<PlatformWalletResponse> getWalletSummary(
            @PathVariable Long organizationId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.getWalletSummaryByOrganizationId(organizationId, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SYSTEM_RECORD_USAGE_CHARGE")
    @PostMapping("/usage/charge")
    @Operation(summary = "Record a platform action usage charge and deduct from prepaid wallet when applicable")
    public ResponseEntity<PlatformWalletResponse> recordUsageCharge(
            @RequestBody RecordPlatformUsageChargeRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.recordUsageCharge(request, locale, "SYSTEM");
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SYSTEM_GET_PUBLIC_PRICING_CATALOG")
    @GetMapping("/pricing-catalog")
    @Operation(summary = "Public pricing catalog for the marketing landing page (active packages and per-action charges)")
    public ResponseEntity<PlatformWalletResponse> getPublicPricingCatalog(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.getPublicPricingCatalog(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
