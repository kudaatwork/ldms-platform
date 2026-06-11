package projectlx.billing.payments.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.billing.payments.utils.requests.LockCurrencyConversionRequest;
import projectlx.billing.payments.service.processor.api.CurrencyManagementServiceProcessor;
import projectlx.billing.payments.utils.responses.CurrencyResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-billing-payments/v1/system/billing")
@Tag(name = "Billing System Resource", description = "Inter-service billing and currency resolution")
@RequiredArgsConstructor
public class BillingSystemResource {

    private final CurrencyManagementServiceProcessor currencyManagementServiceProcessor;

    @Auditable(action = "SYSTEM_GET_ORGANIZATION_CURRENCY_CONTEXT")
    @GetMapping("/organization/{organizationId}/currency-context")
    @Operation(summary = "Resolve functional currency for an organisation (inventory, PO defaults)")
    public CurrencyResponse getOrganizationCurrencyContext(
            @PathVariable("organizationId") Long organizationId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return currencyManagementServiceProcessor.getOrganizationCurrencyContextByOrganizationId(organizationId, locale);
    }

    @Auditable(action = "SYSTEM_LOCK_CURRENCY_CONVERSION")
    @PostMapping("/convert-and-lock")
    @Operation(summary = "Lock spot rate at transaction date and convert to organisation functional currency")
    public CurrencyResponse lockConversionForOrganization(
            @RequestBody LockCurrencyConversionRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        return currencyManagementServiceProcessor.lockConversionForOrganization(request, locale, "SYSTEM");
    }
}
