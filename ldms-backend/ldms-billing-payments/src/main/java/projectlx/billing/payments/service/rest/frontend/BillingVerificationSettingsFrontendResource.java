package projectlx.billing.payments.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.billing.payments.service.processor.api.BillingVerificationSettingsServiceProcessor;
import projectlx.billing.payments.utils.requests.UpdateBillingVerificationPolicyRequest;
import projectlx.billing.payments.utils.responses.BillingVerificationSettingsResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-billing-payments/v1/frontend/billing-settings")
@Tag(name = "Billing Settings", description = "Organisation billing verification policy")
@RequiredArgsConstructor
public class BillingVerificationSettingsFrontendResource {

    private final BillingVerificationSettingsServiceProcessor billingVerificationSettingsServiceProcessor;

    @Auditable(action = "GET_BILLING_VERIFICATION_POLICY")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/verification-policy")
    @Operation(summary = "Get payment verification policy",
            description = "Returns how many sequential billing approvers are required (1-3).")
    public ResponseEntity<BillingVerificationSettingsResponse> getVerificationPolicy(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BillingVerificationSettingsResponse response =
                billingVerificationSettingsServiceProcessor.getVerificationPolicy(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "UPDATE_BILLING_VERIFICATION_POLICY")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/verification-policy")
    @Operation(summary = "Update payment verification policy",
            description = "Sets how many sequential billing approvers must verify customer payments (1-3).")
    public ResponseEntity<BillingVerificationSettingsResponse> updateVerificationPolicy(
            @Valid @RequestBody UpdateBillingVerificationPolicyRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BillingVerificationSettingsResponse response =
                billingVerificationSettingsServiceProcessor.updateVerificationPolicy(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
