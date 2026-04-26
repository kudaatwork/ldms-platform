package projectlx.user.management.business.auditable.api;

import projectlx.user.management.model.UserType;
import java.util.Locale;

public interface UserTypeServiceAuditable {
    UserType create(UserType userType, Locale locale, String username);
    UserType update(UserType userType, Locale locale, String username);
    UserType delete(UserType userType, Locale locale, String username);
}
