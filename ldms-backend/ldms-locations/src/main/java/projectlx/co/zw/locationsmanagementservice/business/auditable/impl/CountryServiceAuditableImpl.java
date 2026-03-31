package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.CountryServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.repository.CountryRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class CountryServiceAuditableImpl implements CountryServiceAuditable {

    private final CountryRepository countryRepository;

    @Override
    public Country create(Country country, Locale locale, String username) {
        return countryRepository.save(country);
    }

    @Override
    public Country update(Country country, Locale locale, String username) {
        return countryRepository.save(country);
    }

    @Override
    public Country delete(Country country, Locale locale, String username) {
        return countryRepository.save(country);
    }
}