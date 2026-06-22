package projectlx.billing.payments.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.billing.payments.service.processor.api.PlatformWalletBillingServiceProcessor;
import projectlx.billing.payments.utils.requests.CreditOrganizationWalletRequest;
import projectlx.billing.payments.utils.requests.SavePlatformActionChargeRequest;
import projectlx.billing.payments.utils.requests.SaveSubscriptionPackageRequest;
import projectlx.billing.payments.utils.responses.PlatformWalletResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-billing-payments/v1/backoffice/platform-wallet")
@Tag(name = "Platform Wallet Backoffice Resource", description = "Admin configuration for action charges and subscription packages")
@RequiredArgsConstructor
public class PlatformWalletBackofficeResource {

    private final PlatformWalletBillingServiceProcessor platformWalletBillingServiceProcessor;

    @Auditable(action = "BACKOFFICE_LIST_ACTION_CHARGES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/action-charges")
    @Operation(summary = "List platform-wide per-action charges (cents)")
    public ResponseEntity<PlatformWalletResponse> listActionCharges(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.listActionCharges(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_SAVE_ACTION_CHARGE")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/action-charges")
    public ResponseEntity<PlatformWalletResponse> saveActionCharge(
            @RequestBody SavePlatformActionChargeRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.saveActionCharge(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_DELETE_ACTION_CHARGE")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/action-charges/{chargeId}")
    @Operation(summary = "Soft-delete a platform action charge")
    public ResponseEntity<PlatformWalletResponse> deleteActionCharge(
            @PathVariable Long chargeId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.deleteActionCharge(chargeId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_LIST_SUBSCRIPTION_PACKAGES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/subscription-packages")
    public ResponseEntity<PlatformWalletResponse> listSubscriptionPackages(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.listSubscriptionPackages(locale, false);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_SAVE_SUBSCRIPTION_PACKAGE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/subscription-packages")
    public ResponseEntity<PlatformWalletResponse> saveSubscriptionPackage(
            @RequestBody SaveSubscriptionPackageRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.saveSubscriptionPackage(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_DELETE_SUBSCRIPTION_PACKAGE")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/subscription-packages/{packageId}")
    @Operation(summary = "Soft-delete a subscription package (blocked when assigned to an organisation)")
    public ResponseEntity<PlatformWalletResponse> deleteSubscriptionPackage(
            @PathVariable Long packageId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.deleteSubscriptionPackage(packageId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_LIST_PENDING_WALLET_DEPOSITS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/deposits/pending")
    public ResponseEntity<PlatformWalletResponse> listPendingDeposits(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.listPendingDeposits(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_LIST_CONFIRMED_WALLET_DEPOSITS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/deposits/confirmed")
    @Operation(summary = "List confirmed wallet deposits (approved transaction history)")
    public ResponseEntity<PlatformWalletResponse> listConfirmedDeposits(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.listConfirmedDeposits(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_CONFIRM_WALLET_DEPOSIT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/deposits/{depositId}/confirm")
    public ResponseEntity<PlatformWalletResponse> confirmWalletDeposit(
            @PathVariable Long depositId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.confirmWalletDeposit(depositId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_REJECT_WALLET_DEPOSIT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/deposits/{depositId}/reject")
    public ResponseEntity<PlatformWalletResponse> rejectWalletDeposit(
            @PathVariable Long depositId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.rejectWalletDeposit(depositId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_CREDIT_ORGANIZATION_WALLET")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/organizations/credit")
    @Operation(summary = "Credit an organisation prepaid wallet (admin POP confirmation)")
    public ResponseEntity<PlatformWalletResponse> creditOrganizationWallet(
            @RequestBody CreditOrganizationWalletRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PlatformWalletResponse response = platformWalletBillingServiceProcessor.creditOrganizationWallet(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
