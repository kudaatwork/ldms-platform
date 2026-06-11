package projectlx.billing.payments.business.validator.api;

import projectlx.billing.payments.utils.requests.ConvertCurrencyRequest;
import projectlx.billing.payments.utils.requests.CreateExchangeRateRequest;
import projectlx.billing.payments.utils.requests.LockCurrencyConversionRequest;
import projectlx.billing.payments.utils.requests.SaveCountryCurrencySettingRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationCurrencySettingRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface CurrencyManagementServiceValidator {
    ValidatorDto isSaveCountryCurrencySettingRequestValid(SaveCountryCurrencySettingRequest request, Locale locale);
    ValidatorDto isCreateExchangeRateRequestValid(CreateExchangeRateRequest request, Locale locale);
    ValidatorDto isSaveOrganizationCurrencySettingRequestValid(SaveOrganizationCurrencySettingRequest request, Locale locale);
    ValidatorDto isConvertCurrencyRequestValid(ConvertCurrencyRequest request, Locale locale);
    ValidatorDto isLockCurrencyConversionRequestValid(LockCurrencyConversionRequest request, Locale locale);
}
