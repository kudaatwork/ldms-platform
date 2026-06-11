package projectlx.billing.payments.service.rest.backoffice;

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
import org.springframework.web.bind.annotation.RestController;
import projectlx.billing.payments.service.processor.api.CurrencyManagementServiceProcessor;
import projectlx.billing.payments.utils.requests.CreateExchangeRateRequest;
import projectlx.billing.payments.utils.requests.SaveCountryCurrencySettingRequest;
import projectlx.billing.payments.utils.responses.CurrencyResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-billing-payments/v1/backoffice/billing")
@Tag(name = "Billing Backoffice Resource", description = "Platform-wide billing and currency administration")
@RequiredArgsConstructor
public class BillingBackofficeResource {

    private final CurrencyManagementServiceProcessor currencyManagementServiceProcessor;

    @Auditable(action = "BACKOFFICE_LIST_CURRENCIES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/currencies")
    @Operation(summary = "List supported currencies")
    public ResponseEntity<CurrencyResponse> listCurrencies(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        CurrencyResponse response = currencyManagementServiceProcessor.listCurrencies(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_LIST_COUNTRY_CURRENCY_SETTINGS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/country-currency-settings")
    public ResponseEntity<CurrencyResponse> listCountryCurrencySettings(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        CurrencyResponse response = currencyManagementServiceProcessor.listCountryCurrencySettings(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_SAVE_COUNTRY_CURRENCY_SETTING")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/country-currency-settings")
    public ResponseEntity<CurrencyResponse> saveCountryCurrencySetting(
            @RequestBody SaveCountryCurrencySettingRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CurrencyResponse response = currencyManagementServiceProcessor.saveCountryCurrencySetting(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_LIST_EXCHANGE_RATES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/exchange-rates")
    public ResponseEntity<CurrencyResponse> listExchangeRates(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        CurrencyResponse response = currencyManagementServiceProcessor.listExchangeRates(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_CREATE_EXCHANGE_RATE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/exchange-rates")
    public ResponseEntity<CurrencyResponse> createExchangeRate(
            @RequestBody CreateExchangeRateRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CurrencyResponse response = currencyManagementServiceProcessor.createExchangeRate(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
