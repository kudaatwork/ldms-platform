package projectlx.user.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.user.management.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.business.logic.api.UserAccountService;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserPassword;
import projectlx.user.management.model.UserSecurityDetails;
import projectlx.user.management.model.UserType;
import projectlx.user.management.repository.UserAccountRepository;
import projectlx.user.management.repository.UserPasswordRepository;
import projectlx.user.management.repository.UserSecurityRepository;
import projectlx.user.management.repository.UserTypeRepository;
import projectlx.user.management.utils.dtos.UserDto;
import projectlx.user.management.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.utils.requests.ProvisionDriverPlatformUserRequest;
import projectlx.user.management.utils.responses.UserAccountResponse;
import projectlx.user.management.utils.responses.UserResponse;
import projectlx.user.management.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Provisions a fleet driver as a platform user with temporary credentials.
 * Mirrors {@link OrganizationContactPersonProvisioner} but simplified for drivers:
 * <ul>
 *   <li>Creates (or reuses) a {@code FLEET_DRIVER} user linked to the organisation.</li>
 *   <li>Issues a temporary username and password (must-change on first login).</li>
 *   <li>Assigns the driver to the org-scoped {@code Driver} user group.</li>
 *   <li>Returns plain-text credentials in the response for downstream email dispatch.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class DriverPlatformProvisioner {

    private static final Logger log = LoggerFactory.getLogger(DriverPlatformProvisioner.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_USERNAME_ATTEMPTS = 12;
    private static final String USER_TYPE_FLEET_DRIVER = "FLEET_DRIVER";

    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final UserTypeRepository userTypeRepository;
    private final UserServiceAuditable userServiceAuditable;
    private final UserPasswordServiceAuditable userPasswordServiceAuditable;
    private final UserSecurityServiceAuditable userSecurityServiceAuditable;
    private final UserAccountService userAccountService;
    private final DriverUserGroupSupport driverUserGroupSupport;
    private final UsernameUniquenessSupport usernameUniquenessSupport;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    /**
     * Provisions driver platform access in a single transaction.
     * Returns a {@link UserResponse} with {@code temporaryUsername} and
     * {@code temporaryPassword} populated in the DTO.
     *
     * @param request provision request (organizationId + email/names required)
     * @param locale  request locale for error messages
     * @param actor   username of the caller (e.g. a fleet manager)
     * @return response with credentials or error details
     */
    @Transactional
    public UserResponse provisionWithTemporaryCredentials(
            ProvisionDriverPlatformUserRequest request, Locale locale, String actor) {
        try {
            return provisionInternal(request, locale, actor);
        } catch (Exception ex) {
            log.error("Driver platform provisioning failed: {}", ex.getMessage(), ex);
            return buildError(500, List.of("Driver platform provisioning failed: " + ex.getMessage()));
        }
    }

    // ============================================================
    // Core logic
    // ============================================================

    private UserResponse provisionInternal(
            ProvisionDriverPlatformUserRequest request, Locale locale, String actor) {

        // ============================================================
        // STEP 1: Validate required fields
        // ============================================================
        if (request == null) {
            return buildError(400, List.of("Provision request is required."));
        }
        if (request.getOrganizationId() == null || request.getOrganizationId() < 1) {
            return buildError(400, List.of("Organisation id is required."));
        }
        if (!StringUtils.hasText(request.getEmail())
                || !Validators.isValidEmail(request.getEmail().trim())) {
            return buildError(400, List.of("A valid driver email address is required."));
        }
        if (!StringUtils.hasText(request.getFirstName())
                || !StringUtils.hasText(request.getLastName())) {
            return buildError(400, List.of("Driver first and last name are required."));
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // ============================================================
        // STEP 2: Resolve user — existingUserId > email lookup > create
        // ============================================================
        User user = resolveOrCreateUser(request, email, locale, actor);
        if (user == null) {
            return buildError(500, List.of("Failed to resolve or create driver user account."));
        }

        // ============================================================
        // STEP 3: Ensure login artifacts (account, password, security)
        // ============================================================
        UserResponse artifactError = ensureLoginArtifacts(user, locale, actor);
        if (!artifactError.isSuccess()) {
            return artifactError;
        }
        user = userRepository.findByIdAndEntityStatusNot(user.getId(), EntityStatus.DELETED)
                .orElse(user);

        // ============================================================
        // STEP 4: Issue temporary credentials
        // ============================================================
        String temporaryUsername = resolveUniqueTemporaryUsername(user.getOrganizationId(), user.getId());
        String temporaryPassword = OrganizationContactCredentialsIssuer.generateCompliantPassword();

        user.setUsername(temporaryUsername);
        user.setMustChangeCredentials(true);
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        driverUserGroupSupport.assignDriverGroup(user);
        User savedUser = userServiceAuditable.update(user, locale, actor);

        UserPassword passwordRow = userPasswordRepository
                .findByUserIdAndEntityStatusNot(savedUser.getId(), EntityStatus.DELETED)
                .orElse(null);
        if (passwordRow == null) {
            passwordRow = new UserPassword();
            passwordRow.setUser(savedUser);
            passwordRow.setIsPasswordExpired(false);
            passwordRow.setExpiryDate(LocalDateTime.now().plusDays(90));
            passwordRow.setPassword(bCryptPasswordEncoder.encode(temporaryPassword));
            userPasswordServiceAuditable.create(passwordRow, locale, actor);
        } else {
            passwordRow.setPassword(bCryptPasswordEncoder.encode(temporaryPassword));
            passwordRow.setIsPasswordExpired(false);
            passwordRow.setExpiryDate(LocalDateTime.now().plusDays(90));
            userPasswordServiceAuditable.update(passwordRow, locale, actor);
        }

        log.info("Issued temporary driver credentials for org={} userId={}",
                request.getOrganizationId(), savedUser.getId());

        // ============================================================
        // STEP 5: Build response with credentials
        // ============================================================
        UserDto dto = new UserDto();
        dto.setId(savedUser.getId());
        dto.setOrganizationId(savedUser.getOrganizationId());
        dto.setUsername(temporaryUsername);
        dto.setEmail(savedUser.getEmail());
        dto.setFirstName(savedUser.getFirstName());
        dto.setLastName(savedUser.getLastName());
        dto.setMustChangeCredentials(true);

        UserResponse response = new UserResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Driver platform access provisioned.");
        response.setUserDto(dto);
        response.setTemporaryUsername(temporaryUsername);
        response.setTemporaryPassword(temporaryPassword);
        return response;
    }

    // ============================================================
    // User resolution
    // ============================================================

    private User resolveOrCreateUser(
            ProvisionDriverPlatformUserRequest request, String email, Locale locale, String actor) {

        if (request.getExistingUserId() != null && request.getExistingUserId() > 0) {
            Optional<User> existing = userRepository.findByIdAndEntityStatusNot(
                    request.getExistingUserId(), EntityStatus.DELETED);
            if (existing.isPresent()) {
                User u = existing.get();
                applyProfileFromRequest(u, request, email);
                if (u.getOrganizationId() == null) {
                    u.setOrganizationId(request.getOrganizationId());
                }
                return userServiceAuditable.update(u, locale, actor);
            }
        }

        Optional<User> byEmail = userRepository.findByEmailAndEntityStatusNot(email, EntityStatus.DELETED);
        if (byEmail.isPresent()) {
            User u = byEmail.get();
            if (u.getOrganizationId() != null && !u.getOrganizationId().equals(request.getOrganizationId())) {
                log.warn("Driver email {} already linked to org {}", email, u.getOrganizationId());
                return null;
            }
            applyProfileFromRequest(u, request, email);
            if (u.getOrganizationId() == null) {
                u.setOrganizationId(request.getOrganizationId());
            }
            return userServiceAuditable.update(u, locale, actor);
        }

        return createNewDriverUser(request, email, locale, actor);
    }

    private User createNewDriverUser(
            ProvisionDriverPlatformUserRequest request, String email, Locale locale, String actor) {
        User user = new User();
        user.setOrganizationId(request.getOrganizationId());
        user.setEmail(email);
        user.setUsername(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmailVerified(false);
        user.setMustChangeCredentials(false);
        user.setOrganizationKycApprover(false);
        user.setProcurementApprover(false);
        user.setShipmentFleetAllocator(false);
        user.setUserType(resolveFleetDriverUserType(locale, actor));
        return userServiceAuditable.create(user, locale, actor);
    }

    private void applyProfileFromRequest(
            User user, ProvisionDriverPlatformUserRequest request, String email) {
        user.setEmail(email);
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName().trim());
        }
        if (StringUtils.hasText(request.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
    }

    // ============================================================
    // Login artifacts
    // ============================================================

    private UserResponse ensureLoginArtifacts(User user, Locale locale, String actor) {
        if (!hasActiveUserAccount(user.getId())) {
            CreateUserAccountRequest accountRequest = new CreateUserAccountRequest();
            accountRequest.setUserId(user.getId());
            accountRequest.setIsAccountLocked(false);
            accountRequest.setPhoneNumber(resolveAccountPhone(user));
            UserAccountResponse accountResponse = userAccountService.create(accountRequest, locale, actor);
            if (!accountResponse.isSuccess()) {
                return buildError(400, List.of(accountResponse.getMessage() != null
                        ? accountResponse.getMessage() : "Failed to create user account."));
            }
        }
        if (!hasActiveUserPassword(user.getId())) {
            UserPassword pw = new UserPassword();
            pw.setUser(user);
            pw.setPassword(bCryptPasswordEncoder.encode(
                    OrganizationContactCredentialsIssuer.generateCompliantPassword()));
            pw.setIsPasswordExpired(false);
            pw.setExpiryDate(LocalDateTime.now().plusDays(90));
            userPasswordServiceAuditable.create(pw, locale, actor);
        }
        if (!hasActiveUserSecurity(user.getId())) {
            UserSecurityDetails securityDetails = defaultSecurityDetails();
            CreateUserSecurityRequest securityRequest = new CreateUserSecurityRequest();
            securityRequest.setUserId(user.getId());
            securityRequest.setSecurityQuestion_1(securityDetails.getSecurityQuestion_1());
            securityRequest.setSecurityQuestion_2(securityDetails.getSecurityQuestion_2());
            securityRequest.setSecurityAnswer_1(securityDetails.getSecurityAnswer_1());
            securityRequest.setSecurityAnswer_2(securityDetails.getSecurityAnswer_2());
            securityRequest.setTwoFactorAuthSecret(securityDetails.getTwoFactorAuthSecret());
            securityRequest.setIsTwoFactorEnabled(Boolean.FALSE);
            // UserSecurityServiceAuditable.create expects the entity, not a request — build directly
            projectlx.user.management.model.UserSecurity security =
                    new projectlx.user.management.model.UserSecurity();
            security.setUser(user);
            security.setSecurityQuestion_1(securityDetails.getSecurityQuestion_1());
            security.setSecurityAnswer_1(securityDetails.getSecurityAnswer_1());
            security.setSecurityQuestion_2(securityDetails.getSecurityQuestion_2());
            security.setSecurityAnswer_2(securityDetails.getSecurityAnswer_2());
            security.setTwoFactorAuthSecret(securityDetails.getTwoFactorAuthSecret());
            security.setIsTwoFactorEnabled(Boolean.FALSE);
            userSecurityServiceAuditable.create(security, locale, actor);
        }
        UserResponse ok = new UserResponse();
        ok.setSuccess(true);
        ok.setStatusCode(200);
        return ok;
    }

    private boolean hasActiveUserAccount(Long userId) {
        return userId != null
                && userAccountRepository.findByUser_IdAndEntityStatusNot(userId, EntityStatus.DELETED).isPresent();
    }

    private boolean hasActiveUserPassword(Long userId) {
        return userId != null
                && userPasswordRepository.findByUserIdAndEntityStatusNot(userId, EntityStatus.DELETED).isPresent();
    }

    private boolean hasActiveUserSecurity(Long userId) {
        return userId != null
                && userSecurityRepository.findByUser_IdAndEntityStatusNot(userId, EntityStatus.DELETED).isPresent();
    }

    private String resolveAccountPhone(User user) {
        String phone = user.getPhoneNumber() != null ? user.getPhoneNumber().trim() : null;
        if (StringUtils.hasText(phone)) {
            return phone;
        }
        long suffix = (user.getId() != null ? user.getId() : 0L);
        return "+263771" + String.format("%06d", suffix % 1_000_000L);
    }

    private static UserSecurityDetails defaultSecurityDetails() {
        UserSecurityDetails details = new UserSecurityDetails();
        details.setSecurityQuestion_1("Please set your first security question (profile).");
        details.setSecurityAnswer_1("TEMP-A1-" + UUID.randomUUID());
        details.setSecurityQuestion_2("Please set your second security question (profile).");
        details.setSecurityAnswer_2("TEMP-A2-" + UUID.randomUUID());
        details.setTwoFactorAuthSecret("TOTP-" + UUID.randomUUID().toString().replace("-", ""));
        details.setIsTwoFactorEnabled(Boolean.FALSE);
        return details;
    }

    // ============================================================
    // Username generation
    // ============================================================

    private String resolveUniqueTemporaryUsername(Long organizationId, Long userId) {
        for (int attempt = 0; attempt < MAX_USERNAME_ATTEMPTS; attempt++) {
            String candidate = generateDriverUsername(organizationId, userId);
            if (usernameUniquenessSupport.isAvailable(candidate, userId)) {
                return candidate;
            }
        }
        return generateDriverUsername(organizationId, userId)
                + Integer.toString(SECURE_RANDOM.nextInt(900_000) + 100_000, 36);
    }

    private static String generateDriverUsername(Long organizationId, Long userId) {
        String suffix = Integer.toString(SECURE_RANDOM.nextInt(900_000) + 100_000, 36);
        long orgPart = organizationId != null ? organizationId : 0L;
        long userPart = userId != null ? userId % 1000L : 0L;
        return "drv" + orgPart + "u" + userPart + suffix;
    }

    // ============================================================
    // User type
    // ============================================================

    private UserType resolveFleetDriverUserType(Locale locale, String actor) {
        return userTypeRepository
                .findByUserTypeNameAndEntityStatusNot(USER_TYPE_FLEET_DRIVER, EntityStatus.DELETED)
                .orElseGet(() -> {
                    UserType type = new UserType();
                    type.setUserTypeName(USER_TYPE_FLEET_DRIVER);
                    type.setDescription("Fleet driver with mobile app access");
                    return userTypeRepository.save(type);
                });
    }

    // ============================================================
    // Response helpers
    // ============================================================

    private static UserResponse buildError(int status, List<String> errors) {
        UserResponse response = new UserResponse();
        response.setSuccess(false);
        response.setStatusCode(status);
        response.setMessage(errors != null && !errors.isEmpty() ? errors.get(0) : "Request failed.");
        response.setErrorMessages(errors);
        return response;
    }
}
