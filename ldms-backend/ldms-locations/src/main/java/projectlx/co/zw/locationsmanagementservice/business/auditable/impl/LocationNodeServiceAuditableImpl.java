package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.LocationNodeServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;
import projectlx.co.zw.locationsmanagementservice.repository.LocationNodeRepository;

@RequiredArgsConstructor
public class LocationNodeServiceAuditableImpl implements LocationNodeServiceAuditable {

    private final LocationNodeRepository locationNodeRepository;

    @Override
    public LocationNode create(LocationNode locationNode, Locale locale, String username) {
        return locationNodeRepository.save(locationNode);
    }

    @Override
    public LocationNode update(LocationNode locationNode, Locale locale, String username) {
        return locationNodeRepository.save(locationNode);
    }

    @Override
    public LocationNode delete(LocationNode locationNode, Locale locale, String username) {
        return locationNodeRepository.save(locationNode);
    }
}
