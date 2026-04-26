package projectlx.user.management.business.auditable.impl;

import projectlx.user.management.business.auditable.api.UserAccountServiceAuditable;
import projectlx.user.management.model.UserAccount;
import projectlx.user.management.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@RequiredArgsConstructor
public class UserAccountServiceAuditableImpl implements UserAccountServiceAuditable {
    private final UserAccountRepository userAccountRepository;

    @Override
    public UserAccount create(UserAccount userAccount, Locale locale, String username) {
        return userAccountRepository.save(userAccount);
    }

    @Override
    public UserAccount update(UserAccount userAccount, Locale locale, String username) {
        return userAccountRepository.save(userAccount);
    }

    @Override
    public UserAccount delete(UserAccount userAccount, Locale locale, String username) {
        return userAccountRepository.save(userAccount);
    }
}
