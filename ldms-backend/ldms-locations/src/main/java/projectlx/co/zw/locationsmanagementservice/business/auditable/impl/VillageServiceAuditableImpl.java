package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.VillageServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.Village;
import projectlx.co.zw.locationsmanagementservice.repository.VillageRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class VillageServiceAuditableImpl implements VillageServiceAuditable {

    private final VillageRepository villageRepository;

    @Override
    public Village create(Village village, Locale locale, String username) {
        return villageRepository.save(village);
    }

    @Override
    public Village update(Village village, Locale locale, String username) {
        return villageRepository.save(village);
    }

    @Override
    public Village delete(Village village, Locale locale) {
        return villageRepository.save(village);
    }
}
