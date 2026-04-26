package projectlx.user.management.business.auditable.impl;

import projectlx.user.management.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.model.UserPassword;
import projectlx.user.management.repository.UserPasswordRepository;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@RequiredArgsConstructor
public class UserPasswordServiceAuditableImpl implements UserPasswordServiceAuditable {

    private final UserPasswordRepository userPasswordRepository;

    @Override
    public UserPassword create(UserPassword userPassword, Locale locale, String username) {
        return userPasswordRepository.save(userPassword);
    }

    @Override
    public UserPassword update(UserPassword userPassword, Locale locale, String username) {
        return userPasswordRepository.save(userPassword);
    }

    @Override
    public UserPassword delete(UserPassword userPassword, Locale locale) {
        return userPasswordRepository.save(userPassword);
    }
}
