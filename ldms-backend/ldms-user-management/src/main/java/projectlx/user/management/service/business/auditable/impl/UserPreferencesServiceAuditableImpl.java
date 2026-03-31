package projectlx.user.management.service.business.auditable.impl;

import projectlx.user.management.service.business.auditable.api.UserPreferencesServiceAuditable;
import projectlx.user.management.service.model.UserPreferences;
import projectlx.user.management.service.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@RequiredArgsConstructor
public class UserPreferencesServiceAuditableImpl implements UserPreferencesServiceAuditable {

    private final UserPreferencesRepository userPreferencesRepository;

    @Override
    public UserPreferences create(UserPreferences userPreferences, Locale locale, String username) {
        return userPreferencesRepository.save(userPreferences);
    }

    @Override
    public UserPreferences update(UserPreferences userPreferences, Locale locale, String username) {
        return userPreferencesRepository.save(userPreferences);
    }

    @Override
    public UserPreferences delete(UserPreferences userPreferences, Locale locale) {
        return userPreferencesRepository.save(userPreferences);
    }
}
