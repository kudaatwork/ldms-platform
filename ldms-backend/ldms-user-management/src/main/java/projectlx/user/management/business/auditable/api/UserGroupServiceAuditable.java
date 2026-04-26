package projectlx.user.management.business.auditable.api;

import projectlx.user.management.model.UserGroup;
import java.util.Locale;

public interface UserGroupServiceAuditable {
    UserGroup create(UserGroup userGroup, Locale locale, String username);
    UserGroup update(UserGroup userGroup, Locale locale, String username);
    UserGroup delete(UserGroup userGroup, Locale locale, String username);
}
