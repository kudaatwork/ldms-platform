package projectlx.user.management.service.business.auditable.api;

import projectlx.user.management.service.model.UserPreferences;
import java.util.Locale;

public interface UserPreferencesServiceAuditable {
    UserPreferences create(UserPreferences userPreferences, Locale locale, String username);
    UserPreferences update(UserPreferences userPreferences, Locale locale, String username);
    UserPreferences delete(UserPreferences userAddress, Locale locale);
}
