package projectlx.user.management.service.business.auditable.impl;

import projectlx.user.management.service.business.auditable.api.UserTypeServiceAuditable;
import projectlx.user.management.service.model.UserType;
import projectlx.user.management.service.repository.UserTypeRepository;
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
