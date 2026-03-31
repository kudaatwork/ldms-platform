package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.LocalizedNameServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.LocalizedName;
import projectlx.co.zw.locationsmanagementservice.repository.LocalizedNameRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class LocalizedNameServiceAuditableImpl implements LocalizedNameServiceAuditable {

    private final LocalizedNameRepository localizedNameRepository;

    @Override
    public LocalizedName create(LocalizedName localizedName, Locale locale, String username) {
        return localizedNameRepository.save(localizedName);
    }

    @Override
    public LocalizedName update(LocalizedName localizedName, Locale locale, String username) {
        return localizedNameRepository.save(localizedName);
    }

    @Override
    public LocalizedName delete(LocalizedName localizedName, Locale locale) {
        return localizedNameRepository.save(localizedName);
    }
}