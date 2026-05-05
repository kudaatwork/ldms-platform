package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.CityServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.City;
import projectlx.co.zw.locationsmanagementservice.repository.CityRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class CityServiceAuditableImpl implements CityServiceAuditable {

    private final CityRepository cityRepository;

    @Override
    public City create(City city, Locale locale, String username) {
        return cityRepository.save(city);
    }

    @Override
    public City update(City city, Locale locale, String username) {
        return cityRepository.save(city);
    }

    @Override
    public City delete(City city, Locale locale) {
        return cityRepository.save(city);
    }
}
