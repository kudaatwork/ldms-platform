package projectlx.user.management.service.business.auditable.impl;

import projectlx.user.management.service.business.auditable.api.UserGroupServiceAuditable;
import projectlx.user.management.service.model.UserGroup;
import projectlx.user.management.service.repository.UserGroupRepository;
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
