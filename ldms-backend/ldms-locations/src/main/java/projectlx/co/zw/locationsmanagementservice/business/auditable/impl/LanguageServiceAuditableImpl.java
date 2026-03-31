package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.LanguageServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.Language;
import projectlx.co.zw.locationsmanagementservice.repository.LanguageRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class LanguageServiceAuditableImpl implements LanguageServiceAuditable {

    private final LanguageRepository languageRepository;

    @Override
    public Language create(Language language, Locale locale, String username) {
        return languageRepository.save(language);
    }

    @Override
    public Language update(Language language, Locale locale, String username) {
        return languageRepository.save(language);
    }

    @Override
    public Language delete(Language language, Locale locale) {
        return languageRepository.save(language);
    }
}