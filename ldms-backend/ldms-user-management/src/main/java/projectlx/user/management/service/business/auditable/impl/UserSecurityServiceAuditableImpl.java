package projectlx.user.management.service.business.auditable.impl;

import projectlx.user.management.service.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.service.model.UserSecurity;
import projectlx.user.management.service.repository.UserSecurityRepository;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@RequiredArgsConstructor
public class UserSecurityServiceAuditableImpl implements UserSecurityServiceAuditable {

    private final UserSecurityRepository userSecurityRepository;

    @Override
    public UserSecurity create(UserSecurity userSecurity, Locale locale, String username) {
        return userSecurityRepository.save(userSecurity);
    }

    @Override
    public UserSecurity update(UserSecurity userSecurity, Locale locale, String username) {
        return userSecurityRepository.save(userSecurity);
    }

    @Override
    public UserSecurity delete(UserSecurity userSecurity, Locale locale) {
        return userSecurityRepository.save(userSecurity);
    }
}
