package projectlx.user.management.business.logic.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.business.validator.api.UserServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserPassword;
import projectlx.user.management.repository.UserPasswordRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CompleteCredentialsSetupRequest;
import projectlx.user.management.utils.requests.IssueOrganizationContactCredentialsRequest;
import projectlx.user.management.utils.responses.UsernameAvailabilityResponse;
import projectlx.user.management.utils.responses.UserResponse;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationContactCredentialsIssuerTest {

    private UserRepository userRepository;
    private OrganizationContactAdministratorGroupSupport administratorGroupSupport;
    private UserPasswordRepository userPasswordRepository;
    private UserServiceAuditable userServiceAuditable;
    private UserPasswordServiceAuditable userPasswordServiceAuditable;
    private UserServiceValidator userServiceValidator;
    private UsernameUniquenessSupport usernameUniquenessSupport;
    private MessageService messageService;

    private OrganizationContactCredentialsIssuer issuer;

    private final Locale locale = Locale.ENGLISH;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        administratorGroupSupport = mock(OrganizationContactAdministratorGroupSupport.class);
        userPasswordRepository = mock(UserPasswordRepository.class);
        userServiceAuditable = mock(UserServiceAuditable.class);
        userPasswordServiceAuditable = mock(UserPasswordServiceAuditable.class);
        userServiceValidator = mock(UserServiceValidator.class);
        usernameUniquenessSupport = mock(UsernameUniquenessSupport.class);
        messageService = mock(MessageService.class);

        issuer = new OrganizationContactCredentialsIssuer(
                userRepository,
                administratorGroupSupport,
                userPasswordRepository,
                userServiceAuditable,
                userPasswordServiceAuditable,
                userServiceValidator,
                usernameUniquenessSupport,
                new BCryptPasswordEncoder(),
                messageService);
    }

    @Test
    void issueTemporaryCredentials_assignsAdministratorUserGroup() {
        User user = activeContactUser("contact@org.example", false);
        UserPassword passwordRow = new UserPassword();
        passwordRow.setUser(user);

        IssueOrganizationContactCredentialsRequest request = new IssueOrganizationContactCredentialsRequest();
        request.setOrganizationId(99L);
        request.setContactUserId(user.getId());

        when(userRepository.findByIdAndEntityStatusNot(user.getId(), EntityStatus.DELETED))
                .thenReturn(Optional.of(user));
        when(usernameUniquenessSupport.isAvailable(any(), eq(user.getId()))).thenReturn(true);
        when(userServiceAuditable.update(any(User.class), eq(locale), eq("SYSTEM"))).thenAnswer(inv -> inv.getArgument(0));
        when(userPasswordRepository.findByUserIdAndEntityStatusNot(user.getId(), EntityStatus.DELETED))
                .thenReturn(Optional.of(passwordRow));
        when(userPasswordServiceAuditable.update(any(UserPassword.class), eq(locale), eq("SYSTEM")))
                .thenReturn(passwordRow);

        UserResponse response = issuer.issueTemporaryCredentials(request, locale, "SYSTEM");

        assertTrue(response.isSuccess());
        verify(administratorGroupSupport).assignIfPresent(user);
        assertTrue(user.getMustChangeCredentials());
        assertTrue(user.getEmailVerified());
    }

    @Test
    void checkUsernameAvailability_reportsTakenWhenAnotherUserOwnsName() {
        User user = activeContactUser("lx99u1abc", true);

        when(userRepository.findByUsernameAndEntityStatusNot("lx99u1abc", EntityStatus.DELETED))
                .thenReturn(Optional.of(user));
        when(userServiceValidator.isValidUserName("existing.user")).thenReturn(true);
        when(usernameUniquenessSupport.isAvailable("existing.user", user.getId())).thenReturn(false);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USERNAME_ALREADY_TAKEN.getCode()), any(String[].class), eq(locale)))
                .thenReturn("That username is already in use.");

        UsernameAvailabilityResponse response =
                issuer.checkUsernameAvailability("existing.user", "lx99u1abc", locale);

        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertFalse(response.getAvailable());
    }

    @Test
    void checkUsernameAvailability_reportsAvailableForFreeName() {
        User user = activeContactUser("lx99u1abc", true);

        when(userRepository.findByUsernameAndEntityStatusNot("lx99u1abc", EntityStatus.DELETED))
                .thenReturn(Optional.of(user));
        when(userServiceValidator.isValidUserName("my.permanent")).thenReturn(true);
        when(usernameUniquenessSupport.isAvailable("my.permanent", user.getId())).thenReturn(true);

        UsernameAvailabilityResponse response =
                issuer.checkUsernameAvailability("my.permanent", "lx99u1abc", locale);

        assertTrue(response.isSuccess());
        assertEquals(Boolean.TRUE, response.getAvailable());
    }

    @Test
    void completeCredentialsSetup_rejectsUsernameAlreadyTakenByAnotherUser() {
        User user = activeContactUser("lx99u1abc", true);
        CompleteCredentialsSetupRequest request = setupRequest("existing.user", "Aa1@Password1", "Aa1@Password1");

        when(userRepository.findByUsernameAndEntityStatusNot("lx99u1abc", EntityStatus.DELETED))
                .thenReturn(Optional.of(user));
        when(userServiceValidator.isValidUserName("existing.user")).thenReturn(true);
        when(usernameUniquenessSupport.isAvailable("existing.user", user.getId())).thenReturn(false);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USERNAME_ALREADY_TAKEN.getCode()), any(String[].class), eq(locale)))
                .thenReturn("That username is already in use.");

        UserResponse response = issuer.completeCredentialsSetup(request, "lx99u1abc", locale, "SYSTEM");

        assertFalse(response.isSuccess());
        assertEquals(409, response.getStatusCode());
        verify(userServiceAuditable, never()).update(any(User.class), eq(locale), eq("SYSTEM"));
    }

    @Test
    void completeCredentialsSetup_succeedsWhenUsernameIsAvailable() {
        User user = activeContactUser("lx99u1abc", true);
        UserPassword passwordRow = new UserPassword();
        passwordRow.setUser(user);
        CompleteCredentialsSetupRequest request = setupRequest("my.permanent", "Aa1@Password1", "Aa1@Password1");

        when(userRepository.findByUsernameAndEntityStatusNot("lx99u1abc", EntityStatus.DELETED))
                .thenReturn(Optional.of(user));
        when(userServiceValidator.isValidUserName("my.permanent")).thenReturn(true);
        when(usernameUniquenessSupport.isAvailable("my.permanent", user.getId())).thenReturn(true);
        when(userServiceAuditable.update(any(User.class), eq(locale), eq("SYSTEM"))).thenAnswer(inv -> inv.getArgument(0));
        when(userPasswordRepository.findByUserIdAndEntityStatusNot(user.getId(), EntityStatus.DELETED))
                .thenReturn(Optional.of(passwordRow));
        when(userPasswordServiceAuditable.update(any(UserPassword.class), eq(locale), eq("SYSTEM")))
                .thenReturn(passwordRow);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Updated");

        UserResponse response = issuer.completeCredentialsSetup(request, "lx99u1abc", locale, "SYSTEM");

        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertEquals("my.permanent", user.getUsername());
        assertEquals(Boolean.FALSE, user.getMustChangeCredentials());
    }

    private static User activeContactUser(String username, boolean mustChange) {
        User user = new User();
        user.setId(42L);
        user.setOrganizationId(99L);
        user.setUsername(username);
        user.setEmail("contact@org.example");
        user.setMustChangeCredentials(mustChange);
        user.setEntityStatus(EntityStatus.ACTIVE);
        return user;
    }

    private static CompleteCredentialsSetupRequest setupRequest(
            String newUsername, String newPassword, String confirmPassword) {
        CompleteCredentialsSetupRequest request = new CompleteCredentialsSetupRequest();
        request.setNewUsername(newUsername);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirmPassword);
        return request;
    }
}
