package projectlx.user.management.business.logic.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsernameUniquenessSupportTest {

    private UserRepository userRepository;
    private UsernameUniquenessSupport support;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        support = new UsernameUniquenessSupport(userRepository);
    }

    @Test
    void isAvailable_returnsTrueWhenNoUserHasUsername() {
        when(userRepository.findByUsernameIgnoreCaseAndEntityStatusNot("new.user", EntityStatus.DELETED))
                .thenReturn(Optional.empty());

        assertTrue(support.isAvailable("new.user", 1L));
    }

    @Test
    void isAvailable_returnsTrueWhenOnlySameUserHasUsername() {
        User owner = new User();
        owner.setId(5L);
        when(userRepository.findByUsernameIgnoreCaseAndEntityStatusNot("Owner", EntityStatus.DELETED))
                .thenReturn(Optional.of(owner));

        assertTrue(support.isAvailable("owner", 5L));
    }

    @Test
    void isAvailable_returnsFalseWhenAnotherUserHasUsername() {
        User other = new User();
        other.setId(9L);
        when(userRepository.findByUsernameIgnoreCaseAndEntityStatusNot("Taken", EntityStatus.DELETED))
                .thenReturn(Optional.of(other));

        assertFalse(support.isAvailable("Taken", 5L));
    }
}
