package projectlx.user.management.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPreferencesDetails {
    private String preferredLanguage; // User's preferred language
    private String timezone; // User's timezone
}
