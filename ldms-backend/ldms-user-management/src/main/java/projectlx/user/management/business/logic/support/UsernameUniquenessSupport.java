package projectlx.user.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.repository.UserRepository;

import java.util.Optional;

/**
 * Ensures usernames are not already assigned to another active user (case-insensitive).
 */
@Component
@RequiredArgsConstructor
public class UsernameUniquenessSupport {

    private final UserRepository userRepository;

    /**
     * @param username      candidate username (trimmed before lookup)
     * @param excludeUserId user id to treat as the owner (e.g. current account during credential setup)
     * @return {@code true} when no other non-deleted user has this username (ignoring case)
     */
    public boolean isAvailable(String username, Long excludeUserId) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        Optional<User> existing = userRepository.findByUsernameIgnoreCaseAndEntityStatusNot(
                username.trim(), EntityStatus.DELETED);
        if (existing.isEmpty()) {
            return true;
        }
        return excludeUserId != null && excludeUserId.equals(existing.get().getId());
    }
}
