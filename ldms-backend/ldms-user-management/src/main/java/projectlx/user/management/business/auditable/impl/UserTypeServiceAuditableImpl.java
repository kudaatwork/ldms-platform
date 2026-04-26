package projectlx.user.management.business.auditable.impl;

import projectlx.user.management.business.auditable.api.UserTypeServiceAuditable;
import projectlx.user.management.model.UserType;
import projectlx.user.management.repository.UserTypeRepository;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@RequiredArgsConstructor
public class UserTypeServiceAuditableImpl implements UserTypeServiceAuditable {

    private final UserTypeRepository userTypeRepository;

    @Override
    public UserType create(UserType userType, Locale locale, String username) {
        return userTypeRepository.save(userType);
    }

    @Override
    public UserType update(UserType userType, Locale locale, String username) {
        return userTypeRepository.save(userType);
    }

    @Override
    public UserType delete(UserType userType, Locale locale, String username) {
        return userTypeRepository.save(userType);
    }
}
