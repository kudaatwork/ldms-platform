package projectlx.billing.payments.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.billing.payments.service.processor.api.PlatformWalletBillingServiceProcessor;
import projectlx.billing.payments.utils.requests.CreateWalletDepositRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationBillingSettingRequest;
import projectlx.billing.payments.utils.requests.UsageChargeReportRequest;
import projectlx.billing.payments.utils.responses.PlatformWalletResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-billing-payments/v1/frontend/platform-wallet")
@Tag(name = "Platform Wallet Frontend Resource", description = "Prepaid wallet, billing mode, deposits, and usage reporting")
@RequiredArgsConstructor
public class PlatformWalletFrontendResource {

    private final PlatformWalletBillingServiceProcessor platformWalletBillingServiceProcessor;

    @Auditable(action = "GET_WALLET_SUMMARY")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/summary")
    @Operation(summary = "Wallet balance and billing mode for the signed-in organisation")
    public ResponseEntity<PlatformWalletResponse> getWalletSummary(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.getWalletSummary(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "GET_BILLING_SETTING")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/billing-setting")
    public ResponseEntity<PlatformWalletResponse> getBillingSetting(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.getBillingSetting(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SAVE_BILLING_SETTING")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/billing-setting")
    public ResponseEntity<PlatformWalletResponse> saveBillingSetting(
            @RequestBody SaveOrganizationBillingSettingRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.saveBillingSetting(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_SUBSCRIPTION_PACKAGES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/subscription-packages")
    public ResponseEntity<PlatformWalletResponse> listSubscriptionPackages(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.listSubscriptionPackages(locale, true);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "CREATE_WALLET_DEPOSIT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/deposits")
    public ResponseEntity<PlatformWalletResponse> createWalletDeposit(
            @RequestBody CreateWalletDepositRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.createWalletDeposit(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_WALLET_DEPOSITS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/deposits")
    public ResponseEntity<PlatformWalletResponse> listWalletDeposits(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.listWalletDeposits(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_ACTIVE_ACTION_CHARGES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/action-charges")
    @Operation(summary = "Active platform action charges (read-only catalog for prepaid usage)")
    public ResponseEntity<PlatformWalletResponse> listActiveActionCharges(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.listActiveActionCharges(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_WALLET_TRANSACTIONS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transactions")
    public ResponseEntity<PlatformWalletResponse> listWalletTransactions(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.listRecentWalletTransactions(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "GET_USAGE_CHARGE_REPORT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/usage-report")
    public ResponseEntity<PlatformWalletResponse> getUsageReport(
            @RequestParam(required = false) Long tripId,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        UsageChargeReportRequest request = new UsageChargeReportRequest();
        request.setTripId(tripId);
        request.setSeasonId(seasonId);
        request.setFrom(from);
        request.setTo(to);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.getUsageReport(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
