package projectlx.user.management.business.auditable.impl;

import projectlx.user.management.business.auditable.api.UserRoleServiceAuditable;
import projectlx.user.management.model.UserRole;
import projectlx.user.management.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@RequiredArgsConstructor
public class UserRoleServiceAuditableImpl implements UserRoleServiceAuditable {

    private final UserRoleRepository userRoleRepository;

    @Override
    public UserRole create(UserRole userRole, Locale locale, String username) {
        return userRoleRepository.save(userRole);
    }

    @Override
    public UserRole update(UserRole userRole, Locale locale, String username) {
        return userRoleRepository.save(userRole);
    }

    @Override
    public UserRole delete(UserRole userRole, Locale locale) {
        return userRoleRepository.save(userRole);
    }
}
