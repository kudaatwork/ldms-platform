package projectlx.user.management.business.auditable.impl;

import projectlx.user.management.business.auditable.api.UserGroupServiceAuditable;
import projectlx.user.management.model.UserGroup;
import projectlx.user.management.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@RequiredArgsConstructor
public class UserGroupServiceAuditableImpl implements UserGroupServiceAuditable {

    private final UserGroupRepository userGroupRepository;

    @Override
    public UserGroup create(UserGroup userGroup, Locale locale, String username) {
        return userGroupRepository.save(userGroup);
    }

    @Override
    public UserGroup update(UserGroup userGroup, Locale locale, String username) {
        return userGroupRepository.save(userGroup);
    }

    @Override
    public UserGroup delete(UserGroup userGroup, Locale locale, String username) {
        return userGroupRepository.save(userGroup);
    }
}
