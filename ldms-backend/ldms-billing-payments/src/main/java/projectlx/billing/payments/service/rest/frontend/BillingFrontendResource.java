package projectlx.billing.payments.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.billing.payments.service.processor.api.CurrencyManagementServiceProcessor;
import projectlx.billing.payments.service.processor.api.DriverExpenseServiceProcessor;
import projectlx.billing.payments.service.processor.api.InvoiceServiceProcessor;
import projectlx.billing.payments.service.processor.api.PaymentServiceProcessor;
import projectlx.billing.payments.utils.requests.ApproveDriverExpenseRequest;
import projectlx.billing.payments.utils.requests.ConvertCurrencyRequest;
import projectlx.billing.payments.utils.requests.CreateExchangeRateRequest;
import projectlx.billing.payments.utils.requests.CreatePaymentRequest;
import projectlx.billing.payments.utils.requests.RecordProcurementPaymentRequest;
import projectlx.billing.payments.utils.requests.RejectDriverExpenseRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationCurrencySettingRequest;
import projectlx.billing.payments.utils.requests.SaveCountryCurrencySettingRequest;
import projectlx.billing.payments.utils.responses.CurrencyResponse;
import projectlx.billing.payments.utils.responses.DriverExpenseResponse;
import projectlx.billing.payments.utils.responses.InvoiceResponse;
import projectlx.billing.payments.utils.responses.PaymentResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-billing-payments/v1/frontend/billing")
@Tag(name = "Billing Frontend Resource", description = "Billing, currency, invoices, and payments")
@RequiredArgsConstructor
public class BillingFrontendResource {

    private final CurrencyManagementServiceProcessor currencyManagementServiceProcessor;
    private final InvoiceServiceProcessor invoiceServiceProcessor;
    private final PaymentServiceProcessor paymentServiceProcessor;
    private final DriverExpenseServiceProcessor driverExpenseServiceProcessor;

    @Auditable(action = "GET_ORGANIZATION_CURRENCY_CONTEXT")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/currency-context")
    @Operation(summary = "Resolved currency context for the signed-in organisation")
    public ResponseEntity<CurrencyResponse> getOrganizationCurrencyContext(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CurrencyResponse response = currencyManagementServiceProcessor.getOrganizationCurrencyContext(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "GET_ORGANIZATION_CURRENCY_SETTING")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/organization-currency-setting")
    @Operation(summary = "Organisation functional currency setting")
    public ResponseEntity<CurrencyResponse> getOrganizationCurrencySetting(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CurrencyResponse response = currencyManagementServiceProcessor.getOrganizationCurrencySetting(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SAVE_ORGANIZATION_CURRENCY_SETTING")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/organization-currency-setting")
    @Operation(summary = "Save organisation functional currency (books & inventory base)")
    public ResponseEntity<CurrencyResponse> saveOrganizationCurrencySetting(
            @RequestBody SaveOrganizationCurrencySettingRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CurrencyResponse response = currencyManagementServiceProcessor.saveOrganizationCurrencySetting(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_CURRENCIES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/currencies")
    @Operation(summary = "List supported currencies")
    public ResponseEntity<CurrencyResponse> listCurrencies(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        CurrencyResponse response = currencyManagementServiceProcessor.listCurrencies(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_COUNTRY_CURRENCY_SETTINGS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/country-currency-settings")
    @Operation(summary = "List country base currency settings")
    public ResponseEntity<CurrencyResponse> listCountryCurrencySettings(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        CurrencyResponse response = currencyManagementServiceProcessor.listCountryCurrencySettings(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "SAVE_COUNTRY_CURRENCY_SETTING")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/country-currency-settings")
    @Operation(summary = "Save country base currency setting")
    public ResponseEntity<CurrencyResponse> saveCountryCurrencySetting(
            @RequestBody SaveCountryCurrencySettingRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CurrencyResponse response = currencyManagementServiceProcessor.saveCountryCurrencySetting(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_EXCHANGE_RATES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/exchange-rates")
    @Operation(summary = "List exchange rates")
    public ResponseEntity<CurrencyResponse> listExchangeRates(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        CurrencyResponse response = currencyManagementServiceProcessor.listExchangeRates(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "CREATE_EXCHANGE_RATE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/exchange-rates")
    @Operation(summary = "Create or update exchange rate")
    public ResponseEntity<CurrencyResponse> createExchangeRate(
            @RequestBody CreateExchangeRateRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CurrencyResponse response = currencyManagementServiceProcessor.createExchangeRate(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "CONVERT_CURRENCY")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/convert")
    @Operation(summary = "Convert amount using locked exchange rate snapshot")
    public ResponseEntity<CurrencyResponse> convert(
            @RequestBody ConvertCurrencyRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CurrencyResponse response = currencyManagementServiceProcessor.convert(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_INVOICES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/invoices")
    @Operation(summary = "List invoices for organisation")
    public ResponseEntity<InvoiceResponse> listInvoices(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        InvoiceResponse response = invoiceServiceProcessor.list(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "CREATE_PAYMENT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/payments")
    @Operation(summary = "Record payment against invoice")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentResponse response = paymentServiceProcessor.create(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "RECORD_PROCUREMENT_PAYMENT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/payments/procurement")
    @Operation(summary = "Record procurement payment for a purchase order",
            description = "Accepts system-recorded or externally uploaded proof. Both sources are valid; metadata is always required.")
    public ResponseEntity<PaymentResponse> recordProcurementPayment(
            @RequestBody RecordProcurementPaymentRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentResponse response = paymentServiceProcessor.recordProcurementPayment(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_PAYMENTS_BY_INVOICE")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/invoices/{invoiceId}/payments")
    @Operation(summary = "List payments for invoice")
    public ResponseEntity<PaymentResponse> listPaymentsByInvoice(
            @PathVariable Long invoiceId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentResponse response = paymentServiceProcessor.listByInvoice(invoiceId, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "VERIFY_PAYMENT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/payments/{paymentId}/verify")
    @Operation(summary = "Verify a pending payment", description = "Marks a PENDING payment as VERIFIED/COMPLETED, updates invoice status, and publishes payment.verified event.")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @PathVariable Long paymentId,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PaymentResponse response = paymentServiceProcessor.verifyPayment(paymentId, username, locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "LIST_DRIVER_EXPENSES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/driver-expenses")
    @Operation(summary = "List driver expense reconciliations")
    public ResponseEntity<DriverExpenseResponse> listDriverExpenses(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverExpenseResponse response = driverExpenseServiceProcessor.list(locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "APPROVE_DRIVER_EXPENSE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/driver-expenses/approve")
    @Operation(summary = "Approve driver expense")
    public ResponseEntity<DriverExpenseResponse> approveDriverExpense(
            @RequestBody ApproveDriverExpenseRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverExpenseResponse response = driverExpenseServiceProcessor.approve(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "REJECT_DRIVER_EXPENSE")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/driver-expenses/reject")
    @Operation(summary = "Reject driver expense")
    public ResponseEntity<DriverExpenseResponse> rejectDriverExpense(
            @RequestBody RejectDriverExpenseRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverExpenseResponse response = driverExpenseServiceProcessor.reject(request, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
