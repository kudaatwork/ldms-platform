package projectlx.user.management.service.business.auditable.api;

import projectlx.user.management.service.model.UserAccount;
import java.util.Locale;

public interface UserAccountServiceAuditable {
    UserAccount create(UserAccount userAccount, Locale locale, String username);
    UserAccount update(UserAccount userAccountToBeEdited, Locale locale, String username);
    UserAccount delete(UserAccount userAccount, Locale locale, String username);
}
