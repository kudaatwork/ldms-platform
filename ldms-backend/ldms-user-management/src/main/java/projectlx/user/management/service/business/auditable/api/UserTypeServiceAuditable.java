package projectlx.user.management.service.business.auditable.api;

import projectlx.user.management.service.model.UserType;
import java.util.Locale;

public interface UserTypeServiceAuditable {
    UserType create(UserType userType, Locale locale, String username);
    UserType update(UserType userType, Locale locale, String username);
    UserType delete(UserType userType, Locale locale, String username);
}
