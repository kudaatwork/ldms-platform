package projectlx.billing.payments.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.logic.api.CurrencyManagementService;
import projectlx.billing.payments.service.processor.api.CurrencyManagementServiceProcessor;
import projectlx.billing.payments.utils.requests.ConvertCurrencyRequest;
import projectlx.billing.payments.utils.requests.LockCurrencyConversionRequest;
import projectlx.billing.payments.utils.requests.CreateExchangeRateRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationCurrencySettingRequest;
import projectlx.billing.payments.utils.requests.SaveCountryCurrencySettingRequest;
import projectlx.billing.payments.utils.responses.CurrencyResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class CurrencyManagementServiceProcessorImpl implements CurrencyManagementServiceProcessor {

    private final CurrencyManagementService currencyManagementService;

    @Override
    public CurrencyResponse listCurrencies(Locale locale) {
        return currencyManagementService.listCurrencies(locale);
    }

    @Override
    public CurrencyResponse listCountryCurrencySettings(Locale locale) {
        return currencyManagementService.listCountryCurrencySettings(locale);
    }

    @Override
    public CurrencyResponse saveCountryCurrencySetting(SaveCountryCurrencySettingRequest request, Locale locale, String username) {
        return currencyManagementService.saveCountryCurrencySetting(request, locale, username);
    }

    @Override
    public CurrencyResponse listExchangeRates(Locale locale) {
        return currencyManagementService.listExchangeRates(locale);
    }

    @Override
    public CurrencyResponse createExchangeRate(CreateExchangeRateRequest request, Locale locale, String username) {
        return currencyManagementService.createExchangeRate(request, locale, username);
    }

    @Override
    public CurrencyResponse convert(ConvertCurrencyRequest request, Locale locale, String username) {
        return currencyManagementService.convert(request, locale, username);
    }

    @Override
    public CurrencyResponse lockConversionForOrganization(
            LockCurrencyConversionRequest request,
            Locale locale,
            String username) {
        return currencyManagementService.lockConversionForOrganization(request, locale, username);
    }

    @Override
    public CurrencyResponse getOrganizationCurrencySetting(Locale locale, String username) {
        return currencyManagementService.getOrganizationCurrencySetting(locale, username);
    }

    @Override
    public CurrencyResponse saveOrganizationCurrencySetting(SaveOrganizationCurrencySettingRequest request, Locale locale, String username) {
        return currencyManagementService.saveOrganizationCurrencySetting(request, locale, username);
    }

    @Override
    public CurrencyResponse getOrganizationCurrencyContext(Locale locale, String username) {
        return currencyManagementService.getOrganizationCurrencyContext(locale, username);
    }

    @Override
    public CurrencyResponse getOrganizationCurrencyContextByOrganizationId(Long organizationId, Locale locale) {
        return currencyManagementService.getOrganizationCurrencyContextByOrganizationId(organizationId, locale);
    }
}
