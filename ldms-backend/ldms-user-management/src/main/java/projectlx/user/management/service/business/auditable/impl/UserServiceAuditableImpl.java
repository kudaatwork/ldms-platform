package projectlx.user.management.service.business.auditable.impl;

import projectlx.user.management.service.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.service.model.User;
import projectlx.user.management.service.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@RequiredArgsConstructor
public class UserServiceAuditableImpl implements UserServiceAuditable {

    private final UserRepository userRepository;

    @Override
    public User create(User user, Locale locale, String username) {
        return userRepository.save(user);
    }

    @Override
    public User update(User user, Locale locale, String username) {
        return userRepository.save(user);
    }

    @Override
    public User delete(User user, Locale locale) {
        return userRepository.save(user);
    }
}
