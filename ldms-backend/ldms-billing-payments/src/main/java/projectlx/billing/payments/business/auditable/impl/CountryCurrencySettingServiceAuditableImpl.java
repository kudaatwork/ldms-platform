package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.CountryCurrencySettingServiceAuditable;
import projectlx.billing.payments.model.CountryCurrencySetting;
import projectlx.billing.payments.repository.CountryCurrencySettingRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class CountryCurrencySettingServiceAuditableImpl implements CountryCurrencySettingServiceAuditable {

    private final CountryCurrencySettingRepository countryCurrencySettingRepository;

    @Override
    public CountryCurrencySetting create(CountryCurrencySetting entity, Locale locale, String username) {
        return countryCurrencySettingRepository.save(entity);
    }

    @Override
    public CountryCurrencySetting update(CountryCurrencySetting entity, Locale locale, String username) {
        return countryCurrencySettingRepository.save(entity);
    }

    @Override
    public CountryCurrencySetting delete(CountryCurrencySetting entity, Locale locale) {
        return countryCurrencySettingRepository.save(entity);
    }
}
