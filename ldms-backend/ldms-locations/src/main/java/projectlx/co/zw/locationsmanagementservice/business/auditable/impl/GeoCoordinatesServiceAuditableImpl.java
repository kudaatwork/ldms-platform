package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.GeoCoordinatesServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class GeoCoordinatesServiceAuditableImpl implements GeoCoordinatesServiceAuditable {

    private final GeoCoordinatesRepository geoCoordinatesRepository;

    @Override
    public GeoCoordinates create(GeoCoordinates geoCoordinates, Locale locale, String username) {
        return geoCoordinatesRepository.save(geoCoordinates);
    }

    @Override
    public GeoCoordinates update(GeoCoordinates geoCoordinates, Locale locale, String username) {
        return geoCoordinatesRepository.save(geoCoordinates);
    }

    @Override
    public GeoCoordinates delete(GeoCoordinates geoCoordinates, Locale locale) {
        return geoCoordinatesRepository.save(geoCoordinates);
    }
}