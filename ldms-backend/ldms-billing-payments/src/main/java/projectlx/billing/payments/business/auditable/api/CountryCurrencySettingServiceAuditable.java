package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.CountryCurrencySetting;

import java.util.Locale;

public interface CountryCurrencySettingServiceAuditable {
    CountryCurrencySetting create(CountryCurrencySetting entity, Locale locale, String username);
    CountryCurrencySetting update(CountryCurrencySetting entity, Locale locale, String username);
    CountryCurrencySetting delete(CountryCurrencySetting entity, Locale locale);
}
