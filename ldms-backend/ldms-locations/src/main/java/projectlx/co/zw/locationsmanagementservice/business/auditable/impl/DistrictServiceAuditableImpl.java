package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.DistrictServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class DistrictServiceAuditableImpl implements DistrictServiceAuditable {

    private final DistrictRepository districtRepository;

    @Override
    public District create(District district, Locale locale, String username) {
        return districtRepository.save(district);
    }

    @Override
    public District update(District district, Locale locale, String username) {
        return districtRepository.save(district);
    }

    @Override
    public District delete(District district, Locale locale) {
        return districtRepository.save(district);
    }
}