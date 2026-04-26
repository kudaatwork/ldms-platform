package projectlx.user.management.business.auditable.api;

import projectlx.user.management.model.UserRole;
import java.util.Locale;

public interface UserRoleServiceAuditable {
    UserRole create(UserRole userRole, Locale locale, String username);
    UserRole update(UserRole userRole, Locale locale, String username);
    UserRole delete(UserRole userRole, Locale locale);
}
