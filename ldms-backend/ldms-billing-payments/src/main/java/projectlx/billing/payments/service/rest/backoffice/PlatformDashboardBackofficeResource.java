package projectlx.billing.payments.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.billing.payments.service.processor.api.PlatformDashboardServiceProcessor;
import projectlx.billing.payments.utils.responses.PlatformBillingDashboardResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-billing-payments/v1/backoffice/platform-dashboard")
@Tag(name = "Platform dashboard (backoffice)", description = "Cross-tenant billing metrics for LX administrators")
@RequiredArgsConstructor
public class PlatformDashboardBackofficeResource {

    private final PlatformDashboardServiceProcessor platformDashboardServiceProcessor;

    @Auditable(action = "BACKOFFICE_PLATFORM_BILLING_DASHBOARD")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/invoices")
    @Operation(summary = "Cross-tenant pending invoice total")
    public ResponseEntity<PlatformBillingDashboardResponse> billingDashboard(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        PlatformBillingDashboardResponse response = platformDashboardServiceProcessor.getBillingDashboard(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
