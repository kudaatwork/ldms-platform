package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.OrganizationCurrencySetting;

import java.util.Locale;

public interface OrganizationCurrencySettingServiceAuditable {
    OrganizationCurrencySetting create(OrganizationCurrencySetting entity, Locale locale, String username);
    OrganizationCurrencySetting update(OrganizationCurrencySetting entity, Locale locale, String username);
    OrganizationCurrencySetting delete(OrganizationCurrencySetting entity, Locale locale);
}
