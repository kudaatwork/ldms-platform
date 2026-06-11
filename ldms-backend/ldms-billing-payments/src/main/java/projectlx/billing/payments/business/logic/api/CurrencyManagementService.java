package projectlx.billing.payments.business.logic.api;

import projectlx.billing.payments.utils.requests.ConvertCurrencyRequest;
import projectlx.billing.payments.utils.requests.LockCurrencyConversionRequest;
import projectlx.billing.payments.utils.requests.CreateExchangeRateRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationCurrencySettingRequest;
import projectlx.billing.payments.utils.requests.SaveCountryCurrencySettingRequest;
import projectlx.billing.payments.utils.responses.CurrencyResponse;

import java.util.Locale;

public interface CurrencyManagementService {

    CurrencyResponse listCurrencies(Locale locale);

    CurrencyResponse listCountryCurrencySettings(Locale locale);

    CurrencyResponse saveCountryCurrencySetting(SaveCountryCurrencySettingRequest request, Locale locale, String username);

    CurrencyResponse listExchangeRates(Locale locale);

    CurrencyResponse createExchangeRate(CreateExchangeRateRequest request, Locale locale, String username);

    CurrencyResponse convert(ConvertCurrencyRequest request, Locale locale, String username);

    CurrencyResponse lockConversionForOrganization(LockCurrencyConversionRequest request, Locale locale, String username);

    CurrencyResponse getOrganizationCurrencySetting(Locale locale, String username);

    CurrencyResponse saveOrganizationCurrencySetting(SaveOrganizationCurrencySettingRequest request, Locale locale, String username);

    CurrencyResponse getOrganizationCurrencyContext(Locale locale, String username);

    CurrencyResponse getOrganizationCurrencyContextByOrganizationId(Long organizationId, Locale locale);
}
