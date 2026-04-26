package projectlx.user.management.business.auditable.api;

import projectlx.user.management.model.User;
import java.util.Locale;

public interface UserServiceAuditable {
    User create(User user, Locale locale, String username);
    User update(User user, Locale locale, String username);
    User delete(User user, Locale locale);
}
