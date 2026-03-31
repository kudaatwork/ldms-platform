package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.AdministrativeLevelServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.locationsmanagementservice.repository.AdministrativeLevelRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class AdministrativeLevelServiceAuditableImpl implements AdministrativeLevelServiceAuditable {

    private final AdministrativeLevelRepository administrativeLevelRepository;

    @Override
    public AdministrativeLevel create(AdministrativeLevel administrativeLevel, Locale locale, String username) {
        return administrativeLevelRepository.save(administrativeLevel);
    }

    @Override
    public AdministrativeLevel update(AdministrativeLevel administrativeLevel, Locale locale, String username) {
        return administrativeLevelRepository.save(administrativeLevel);
    }

    @Override
    public AdministrativeLevel delete(AdministrativeLevel administrativeLevel, Locale locale) {
        return administrativeLevelRepository.save(administrativeLevel);
    }
}