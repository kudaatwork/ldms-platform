package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.OrganizationCurrencySettingServiceAuditable;
import projectlx.billing.payments.model.OrganizationCurrencySetting;
import projectlx.billing.payments.repository.OrganizationCurrencySettingRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class OrganizationCurrencySettingServiceAuditableImpl implements OrganizationCurrencySettingServiceAuditable {

    private final OrganizationCurrencySettingRepository organizationCurrencySettingRepository;

    @Override
    public OrganizationCurrencySetting create(OrganizationCurrencySetting entity, Locale locale, String username) {
        return organizationCurrencySettingRepository.save(entity);
    }

    @Override
    public OrganizationCurrencySetting update(OrganizationCurrencySetting entity, Locale locale, String username) {
        return organizationCurrencySettingRepository.save(entity);
    }

    @Override
    public OrganizationCurrencySetting delete(OrganizationCurrencySetting entity, Locale locale) {
        return organizationCurrencySettingRepository.save(entity);
    }
}
