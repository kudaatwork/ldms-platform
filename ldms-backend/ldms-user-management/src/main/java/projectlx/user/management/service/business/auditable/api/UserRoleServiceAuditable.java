package projectlx.user.management.service.business.auditable.api;

import projectlx.user.management.service.model.UserRole;
import java.util.Locale;

public interface UserRoleServiceAuditable {
    UserRole create(UserRole userRole, Locale locale, String username);
    UserRole update(UserRole userRole, Locale locale, String username);
    UserRole delete(UserRole userRole, Locale locale);
}
