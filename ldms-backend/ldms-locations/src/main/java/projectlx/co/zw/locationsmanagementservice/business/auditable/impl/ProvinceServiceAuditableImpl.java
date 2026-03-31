package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.ProvinceServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.locationsmanagementservice.repository.ProvinceRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class ProvinceServiceAuditableImpl implements ProvinceServiceAuditable {

    private final ProvinceRepository provinceRepository;

    @Override
    public Province create(Province province, Locale locale, String username) {
        return provinceRepository.save(province);
    }

    @Override
    public Province update(Province province, Locale locale, String username) {
        return provinceRepository.save(province);
    }

    @Override
    public Province delete(Province province, Locale locale) {
        return provinceRepository.save(province);
    }
}