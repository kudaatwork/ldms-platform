package projectlx.user.management.business.auditable.api;

import projectlx.user.management.model.UserSecurity;
import java.util.Locale;

public interface UserSecurityServiceAuditable {
    UserSecurity create(UserSecurity userSecurity, Locale locale, String username);
    UserSecurity update(UserSecurity userSecurity, Locale locale, String username);
    UserSecurity delete(UserSecurity userSecurity, Locale locale);
}
