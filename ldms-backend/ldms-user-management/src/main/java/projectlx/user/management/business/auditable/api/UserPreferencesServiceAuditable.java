package projectlx.user.management.business.auditable.api;

import projectlx.user.management.model.UserPreferences;
import java.util.Locale;

public interface UserPreferencesServiceAuditable {
    UserPreferences create(UserPreferences userPreferences, Locale locale, String username);
    UserPreferences update(UserPreferences userPreferences, Locale locale, String username);
    UserPreferences delete(UserPreferences userAddress, Locale locale);
}
