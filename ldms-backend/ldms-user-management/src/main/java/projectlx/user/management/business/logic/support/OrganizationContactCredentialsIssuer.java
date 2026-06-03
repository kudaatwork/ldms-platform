package projectlx.user.management.business.logic.support;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.business.validator.api.UserServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserPassword;
import projectlx.user.management.repository.UserPasswordRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.utils.dtos.UserDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CompleteCredentialsSetupRequest;
import projectlx.user.management.utils.requests.IssueOrganizationContactCredentialsRequest;
import projectlx.user.management.utils.responses.UserResponse;

/**
 * Issues temporary portal credentials for organisation contact users after KYC approval,
 * and completes the mandatory username/password change on first sign-in.
 */
@Component
@RequiredArgsConstructor
public class OrganizationContactCredentialsIssuer {

    private static final Logger log = LoggerFactory.getLogger(OrganizationContactCredentialsIssuer.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final UserServiceAuditable userServiceAuditable;
    private final UserPasswordServiceAuditable userPasswordServiceAuditable;
    private final UserServiceValidator userServiceValidator;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final MessageService messageService;

    @Transactional
    public UserResponse issueTemporaryCredentials(
            IssueOrganizationContactCredentialsRequest request, Locale locale, String actor) {
        if (request == null || request.getOrganizationId() == null || request.getOrganizationId() < 1) {
            return buildError(400, List.of("Organisation id is required."));
        }
        User user = resolveContactUser(request.getOrganizationId(), request.getContactUserId());
        if (user == null) {
            return buildError(404, List.of("Organisation contact person user was not found."));
        }
        String temporaryUsername = generateTemporaryUsername(user.getOrganizationId(), user.getId());
        String temporaryPassword = generateCompliantPassword();
        user.setUsername(temporaryUsername);
        user.setMustChangeCredentials(true);
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        User savedUser = userServiceAuditable.update(user, locale, actor);

        UserPassword passwordRow = userPasswordRepository
                .findByUserIdAndEntityStatusNot(savedUser.getId(), EntityStatus.DELETED)
                .orElse(null);
        if (passwordRow == null) {
            passwordRow = new UserPassword();
            passwordRow.setUser(savedUser);
            passwordRow.setIsPasswordExpired(false);
            passwordRow.setExpiryDate(java.time.LocalDateTime.now().plusDays(90));
            passwordRow.setPassword(bCryptPasswordEncoder.encode(temporaryPassword));
            userPasswordServiceAuditable.create(passwordRow, locale, actor);
        } else {
            passwordRow.setPassword(bCryptPasswordEncoder.encode(temporaryPassword));
            passwordRow.setIsPasswordExpired(false);
            passwordRow.setExpiryDate(java.time.LocalDateTime.now().plusDays(90));
            userPasswordServiceAuditable.update(passwordRow, locale, actor);
        }

        log.info(
                "Issued temporary credentials for organisation {} contact user {}",
                request.getOrganizationId(),
                savedUser.getId());

        UserDto dto = new UserDto();
        dto.setId(savedUser.getId());
        dto.setOrganizationId(savedUser.getOrganizationId());
        dto.setUsername(temporaryUsername);
        dto.setEmail(savedUser.getEmail());
        dto.setMustChangeCredentials(true);

        UserResponse response = new UserResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Temporary credentials issued.");
        response.setUserDto(dto);
        response.setTemporaryUsername(temporaryUsername);
        response.setTemporaryPassword(temporaryPassword);
        return response;
    }

    @Transactional
    public UserResponse completeCredentialsSetup(
            CompleteCredentialsSetupRequest request, String currentUsername, Locale locale, String actor) {
        if (request == null) {
            return buildError(400, List.of("Request is required."));
        }
        if (!StringUtils.hasText(currentUsername)) {
            return buildError(401, List.of("Authentication is required."));
        }
        String newUsername = request.getNewUsername() != null ? request.getNewUsername().trim() : "";
        String newPassword = request.getNewPassword() != null ? request.getNewPassword().trim() : "";
        String confirmPassword = request.getConfirmPassword() != null ? request.getConfirmPassword().trim() : "";
        if (!StringUtils.hasText(newUsername) || !userServiceValidator.isValidUserName(newUsername)) {
            return buildError(400, List.of("Choose a valid username (letters, numbers, dots, underscores)."));
        }
        if (!StringUtils.hasText(newPassword) || !Validators.isPasswordValid(newPassword)) {
            return buildError(400, List.of("Password does not meet complexity requirements."));
        }
        if (!newPassword.equals(confirmPassword)) {
            return buildError(400, List.of("Password confirmation does not match."));
        }

        User user = userRepository
                .findByUsernameAndEntityStatusNot(currentUsername.trim(), EntityStatus.DELETED)
                .or(() -> userRepository.findByEmailAndEntityStatusNot(currentUsername.trim().toLowerCase(Locale.ROOT),
                        EntityStatus.DELETED))
                .orElse(null);
        if (user == null) {
            return buildError(404, List.of("User not found."));
        }
        if (!Boolean.TRUE.equals(user.getMustChangeCredentials())) {
            return buildError(400, List.of("Credential setup is not required for this account."));
        }
        if (!newUsername.equalsIgnoreCase(user.getUsername())) {
            Optional<User> taken = userRepository.findByUsernameAndEntityStatusNot(newUsername, EntityStatus.DELETED);
            if (taken.isPresent() && !taken.get().getId().equals(user.getId())) {
                return buildError(409, List.of("Username is already taken."));
            }
        }

        user.setUsername(newUsername);
        user.setMustChangeCredentials(false);
        User savedUser = userServiceAuditable.update(user, locale, actor);

        UserPassword passwordRow = userPasswordRepository
                .findByUserIdAndEntityStatusNot(savedUser.getId(), EntityStatus.DELETED)
                .orElse(null);
        if (passwordRow == null) {
            return buildError(404, List.of("User password record not found."));
        }
        passwordRow.setPassword(bCryptPasswordEncoder.encode(newPassword));
        passwordRow.setIsPasswordExpired(false);
        passwordRow.setExpiryDate(java.time.LocalDateTime.now().plusDays(90));
        userPasswordServiceAuditable.update(passwordRow, locale, actor);

        UserDto dto = new UserDto();
        dto.setId(savedUser.getId());
        dto.setUsername(savedUser.getUsername());
        dto.setEmail(savedUser.getEmail());
        dto.setOrganizationId(savedUser.getOrganizationId());
        dto.setMustChangeCredentials(false);

        String message = messageService.getMessage(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode(), new String[] {}, locale);
        UserResponse response = new UserResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(message);
        response.setUserDto(dto);
        return response;
    }

    private User resolveContactUser(Long organizationId, Long contactUserId) {
        if (contactUserId != null && contactUserId > 0) {
            Optional<User> byId = userRepository.findByIdAndEntityStatusNot(contactUserId, EntityStatus.DELETED);
            if (byId.isPresent() && organizationId.equals(byId.get().getOrganizationId())) {
                return byId.get();
            }
        }
        return userRepository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .stream()
                .filter(user -> user.getOrganizationId() != null)
                .findFirst()
                .orElse(null);
    }

    private static String generateTemporaryUsername(Long organizationId, Long userId) {
        String suffix = Integer.toString(SECURE_RANDOM.nextInt(900_000) + 100_000, 36);
        long orgPart = organizationId != null ? organizationId : 0L;
        long userPart = userId != null ? userId % 1000L : 0L;
        return "lx" + orgPart + "u" + userPart + suffix;
    }

    static String generateCompliantPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "@$!%*?&#";
        String all = upper + lower + digits + special;
        StringBuilder password = new StringBuilder();
        password.append(upper.charAt(SECURE_RANDOM.nextInt(upper.length())));
        password.append(lower.charAt(SECURE_RANDOM.nextInt(lower.length())));
        password.append(digits.charAt(SECURE_RANDOM.nextInt(digits.length())));
        password.append(special.charAt(SECURE_RANDOM.nextInt(special.length())));
        for (int i = 4; i < 16; i++) {
            password.append(all.charAt(SECURE_RANDOM.nextInt(all.length())));
        }
        String candidate = password.toString();
        if (candidate.matches(Constants.PASSWORD_REGEX)) {
            return candidate;
        }
        return "Aa1@" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static UserResponse buildError(int status, List<String> errors) {
        UserResponse response = new UserResponse();
        response.setSuccess(false);
        response.setStatusCode(status);
        response.setMessage(errors != null && !errors.isEmpty() ? errors.get(0) : "Request failed.");
        response.setErrorMessages(errors);
        return response;
    }
}
