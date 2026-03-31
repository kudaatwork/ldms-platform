package projectlx.user.management.service.business.auditable.api;

import projectlx.user.management.service.model.User;
import java.util.Locale;

public interface UserServiceAuditable {
    User create(User user, Locale locale, String username);
    User update(User user, Locale locale, String username);
    User delete(User user, Locale locale);
}
