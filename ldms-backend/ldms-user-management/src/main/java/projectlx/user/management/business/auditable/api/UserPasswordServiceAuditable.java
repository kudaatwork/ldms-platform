package projectlx.user.management.business.auditable.api;

import projectlx.user.management.model.UserPassword;
import java.util.Locale;

public interface UserPasswordServiceAuditable {
    UserPassword create(UserPassword user, Locale locale, String username);
    UserPassword update(UserPassword user, Locale locale, String username);
    UserPassword delete(UserPassword user, Locale locale);
}
