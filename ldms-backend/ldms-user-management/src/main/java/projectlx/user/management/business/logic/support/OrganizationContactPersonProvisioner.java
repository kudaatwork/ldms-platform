package projectlx.user.management.business.logic.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.business.logic.impl.TokenService;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.business.logic.api.UserAccountService;
import projectlx.user.management.business.logic.api.UserPasswordService;
import projectlx.user.management.business.logic.api.UserSecurityService;
import projectlx.user.management.business.logic.api.UserTypeService;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.Gender;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserAccount;
import projectlx.user.management.model.UserSecurityDetails;
import projectlx.user.management.model.UserType;
import projectlx.user.management.model.UserTypeDetails;
import projectlx.user.management.repository.UserAccountRepository;
import projectlx.user.management.repository.UserPasswordRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.UserSecurityRepository;
import projectlx.user.management.repository.UserTypeRepository;
import projectlx.user.management.utils.config.EmailVerificationLinkProperties;
import projectlx.user.management.utils.dtos.UserDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.notifications.UserNotificationTemplateData;
import projectlx.user.management.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.utils.requests.CreateUserPasswordRequest;
import projectlx.user.management.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.utils.requests.NotificationRequest;
import projectlx.user.management.utils.requests.ProvisionOrganizationContactPersonRequest;
import projectlx.user.management.utils.responses.UserAccountResponse;
import projectlx.user.management.utils.responses.UserResponse;
import projectlx.user.management.utils.responses.UserSecurityResponse;
import projectlx.user.management.utils.responses.UserTypeResponse;

/**
 * Creates the organisation contact person as a pending user (email not verified) linked to the organisation.
 */
@Component
@RequiredArgsConstructor
public class OrganizationContactPersonProvisioner {

    private static final Logger log = LoggerFactory.getLogger(OrganizationContactPersonProvisioner.class);
    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";
    private static final String TEMPLATE_CONTACT_VERIFICATION = "ORG_CONTACT_PERSON_VERIFICATION";
    private static final String USER_TYPE_ORGANIZATION_CONTACT = "ORGANIZATION_CONTACT";

    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final UserTypeRepository userTypeRepository;
    private final UserServiceAuditable userServiceAuditable;
    private final OrganizationContactAdministratorGroupSupport administratorGroupSupport;
    private final UserAccountService userAccountService;
    private final UserPasswordService userPasswordService;
    private final UserSecurityService userSecurityService;
    private final UserTypeService userTypeService;
    private final TokenService tokenService;
    private final RabbitTemplate rabbitTemplate;
    private final EmailVerificationLinkProperties emailVerificationLinkProperties;
    private final MessageService messageService;

    @Value("${ldms.user.organization-contact-portal-base-url:http://localhost:4201}")
    private String organizationContactPortalBaseUrl;

    @Value("${ldms.user.admin-portal-base-url:http://localhost:4200}")
    private String adminPortalBaseUrl;

    @Transactional
    public UserResponse provision(ProvisionOrganizationContactPersonRequest request, Locale locale, String actor) {
        try {
            return provisionInternal(request, locale, actor);
        } catch (Exception e) {
            log.error("Organisation contact person provisioning failed: {}", e.getMessage(), e);
            return buildError(500, List.of(
                    "Contact person provisioning failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
        }
    }

    private UserResponse provisionInternal(ProvisionOrganizationContactPersonRequest request, Locale locale, String actor) {
        ValidatorDto validation = validateProvisionRequest(request, locale);
        if (!Boolean.TRUE.equals(validation.getSuccess())) {
            return buildError(400, validation.getErrorMessages());
        }

        if (request.getContactUserId() != null && request.getContactUserId() > 0) {
            return syncLinkedContactPerson(request, locale, actor);
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Optional<User> existing = userRepository.findByEmailAndEntityStatusNot(email, EntityStatus.DELETED);
        if (existing.isPresent()) {
            return handleExistingUser(existing.get(), request, locale, actor);
        }

        User user = new User();
        user.setOrganizationId(request.getOrganizationId());
        user.setEmail(email);
        user.setUsername(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhoneNumber(resolvePhone(request));
        user.setGender(resolveGender(request.getGender()));
        user.setNationalIdNumber(resolveNationalId(request));
        if (StringUtils.hasText(request.getPassportNumber())) {
            user.setPassportNumber(request.getPassportNumber().trim());
        }
        if (request.getNationalIdUploadId() != null && request.getNationalIdUploadId() > 0) {
            user.setNationalIdUploadId(request.getNationalIdUploadId());
        }
        if (request.getPassportUploadId() != null && request.getPassportUploadId() > 0) {
            user.setPassportUploadId(request.getPassportUploadId());
        }
        if (StringUtils.hasText(request.getDateOfBirth())) {
            try {
                user.setDateOfBirth(java.sql.Date.valueOf(request.getDateOfBirth().trim()));
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid contact person date of birth for organisation {}: {}", request.getOrganizationId(),
                        request.getDateOfBirth());
            }
        }
        user.setOrganizationKycApprover(false);
        user.setProcurementApprover(false);
        user.setShipmentFleetAllocator(false);
        user.setEmailVerified(false);
        user.setUserType(resolveOrganizationContactUserType(locale, actor));

        User saved = userServiceAuditable.create(user, locale, actor);
        administratorGroupSupport.assignIfPresent(saved);
        saved = userServiceAuditable.update(saved, locale, actor);

        CreateUserAccountRequest accountRequest = new CreateUserAccountRequest();
        accountRequest.setPhoneNumber(resolveAccountPhoneNumber(saved));
        accountRequest.setIsAccountLocked(false);
        accountRequest.setUserId(saved.getId());
        UserAccountResponse accountResponse = userAccountService.create(accountRequest, locale, actor);
        if (!accountResponse.isSuccess()) {
            return buildError(400, resolveErrorMessages(accountResponse.getErrorMessages(), accountResponse.getMessage()));
        }

        String temporaryPassword = OrganizationContactCredentialsIssuer.generateCompliantPassword();
        CreateUserPasswordRequest passwordRequest = new CreateUserPasswordRequest();
        passwordRequest.setUserId(saved.getId());
        passwordRequest.setPassword(temporaryPassword);
        var passwordResponse = userPasswordService.create(passwordRequest, locale, actor);
        if (!passwordResponse.isSuccess()) {
            return buildError(400, resolveErrorMessages(passwordResponse.getErrorMessages(), passwordResponse.getMessage()));
        }

        UserSecurityDetails securityDetails = defaultSecurityDetails();
        UserSecurityResponse securityResponse = attachSecurity(saved, securityDetails, locale, actor);
        if (!securityResponse.isSuccess()) {
            return buildError(400, resolveErrorMessages(securityResponse.getErrorMessages(), securityResponse.getMessage()));
        }

        User updated = saved;
        if (shouldSendVerificationEmail(request)) {
            String verificationToken = tokenService.generateEmailVerificationToken();
            saved.setVerificationToken(verificationToken);
            updated = userServiceAuditable.update(saved, locale, actor);
            sendContactPersonVerificationEmail(updated, request, verificationToken);
        }

        UserDto dto = new UserDto();
        dto.setId(updated.getId());
        dto.setEmail(updated.getEmail());
        dto.setOrganizationId(updated.getOrganizationId());
        dto.setEmailVerified(updated.getEmailVerified());
        dto.setFirstName(updated.getFirstName());
        dto.setLastName(updated.getLastName());

        String message = messageService.getMessage(I18Code.MESSAGE_USER_CREATED_SUCCESSFULLY.getCode(), new String[] {}, locale);
        UserResponse response = new UserResponse();
        response.setSuccess(true);
        response.setStatusCode(201);
        response.setMessage(message);
        response.setUserDto(dto);
        return response;
    }

    private UserResponse syncLinkedContactPerson(
            ProvisionOrganizationContactPersonRequest request, Locale locale, String actor) {
        User user = userRepository
                .findByIdAndEntityStatusNot(request.getContactUserId(), EntityStatus.DELETED)
                .orElse(null);
        if (user == null) {
            return buildError(404, List.of("Linked contact person user was not found."));
        }
        Long orgId = request.getOrganizationId();
        if (user.getOrganizationId() != null && !user.getOrganizationId().equals(orgId)) {
            log.warn(
                    "Linked contact user {} belongs to organisation {}, not {}",
                    user.getId(),
                    user.getOrganizationId(),
                    orgId);
            return buildError(409, List.of("Contact person email is already linked to another organisation."));
        }
        UserResponse emailConflict = applyContactEmailChange(user, request.getEmail(), locale, actor);
        if (!emailConflict.isSuccess()) {
            return emailConflict;
        }
        user = userRepository.findByIdAndEntityStatusNot(user.getId(), EntityStatus.DELETED).orElse(user);
        applyContactProfileFromRequest(user, request);
        if (user.getOrganizationId() == null) {
            user.setOrganizationId(orgId);
        }
        user = userServiceAuditable.update(user, locale, actor);
        administratorGroupSupport.assignIfPresent(user);
        user = userServiceAuditable.update(user, locale, actor);

        UserResponse artifacts = ensureLoginArtifacts(user, locale, actor);
        if (!artifacts.isSuccess()) {
            return artifacts;
        }
        user = userRepository.findByIdAndEntityStatusNot(user.getId(), EntityStatus.DELETED).orElse(user);

        if (isFullyOnboardedContact(user)) {
            log.info(
                    "Linked contact user {} for organisation {} is already verified — profile synced, no refresh needed",
                    user.getId(),
                    orgId);
            return buildSuccess(user, locale, 200);
        }

        return handleExistingUser(user, request, locale, actor);
    }

    private UserResponse handleExistingUser(User user, ProvisionOrganizationContactPersonRequest request, Locale locale,
            String actor) {
        Long orgId = request.getOrganizationId();
        if (user.getOrganizationId() != null && !user.getOrganizationId().equals(orgId)) {
            log.warn("Contact person email {} already linked to organisation {}", user.getEmail(), user.getOrganizationId());
            return buildError(409, List.of("Contact person email is already linked to another organisation."));
        }
        UserResponse emailConflict = applyContactEmailChange(user, request.getEmail(), locale, actor);
        if (!emailConflict.isSuccess()) {
            return emailConflict;
        }
        user = userRepository.findByIdAndEntityStatusNot(user.getId(), EntityStatus.DELETED).orElse(user);
        applyContactProfileFromRequest(user, request);
        if (user.getOrganizationId() == null || !orgId.equals(user.getOrganizationId())) {
            user.setOrganizationId(orgId);
            user = userServiceAuditable.update(user, locale, actor);
        }
        administratorGroupSupport.assignIfPresent(user);
        user = userServiceAuditable.update(user, locale, actor);
        UserResponse artifacts = ensureLoginArtifacts(user, locale, actor);
        if (!artifacts.isSuccess()) {
            return artifacts;
        }
        user = userRepository.findByIdAndEntityStatusNot(user.getId(), EntityStatus.DELETED).orElse(user);
        if (isFullyOnboardedContact(user)) {
            log.info(
                    "Contact user {} for organisation {} is already verified — skipping verification refresh",
                    user.getId(),
                    orgId);
            return buildSuccess(user, locale, 200);
        }
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            userServiceAuditable.update(user, locale, actor);
            return buildSuccess(user, locale, 200);
        }
        if (shouldSendVerificationEmail(request)) {
            String verificationToken = tokenService.generateEmailVerificationToken();
            user.setVerificationToken(verificationToken);
            user.setEmailVerified(false);
            User updated = userServiceAuditable.update(user, locale, actor);
            sendContactPersonVerificationEmail(updated, request, verificationToken);
            return buildSuccess(updated, locale, 200);
        }
        user.setEmailVerified(false);
        User updated = userServiceAuditable.update(user, locale, actor);
        return buildSuccess(updated, locale, 200);
    }

    private UserResponse applyContactEmailChange(User user, String rawEmail, Locale locale, String actor) {
        String email = rawEmail != null ? rawEmail.trim().toLowerCase(Locale.ROOT) : "";
        if (!StringUtils.hasText(email)) {
            return buildError(400, List.of("A valid contact person email is required."));
        }
        String currentEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase(Locale.ROOT) : "";
        if (email.equals(currentEmail)) {
            UserResponse ok = new UserResponse();
            ok.setSuccess(true);
            ok.setStatusCode(200);
            return ok;
        }
        Optional<User> emailOwner = userRepository.findByEmailAndEntityStatusNot(email, EntityStatus.DELETED);
        if (emailOwner.isPresent() && !emailOwner.get().getId().equals(user.getId())) {
            return buildError(409, List.of("Contact person email is already linked to another organisation."));
        }
        user.setEmail(email);
        if (!isFullyOnboardedContact(user)) {
            user.setUsername(email);
        }
        userServiceAuditable.update(user, locale, actor);
        UserResponse ok = new UserResponse();
        ok.setSuccess(true);
        ok.setStatusCode(200);
        return ok;
    }

    private void applyContactProfileFromRequest(User user, ProvisionOrganizationContactPersonRequest request) {
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName().trim());
        }
        String phone = resolvePhone(request);
        if (StringUtils.hasText(phone)) {
            user.setPhoneNumber(phone);
        }
        if (request.getGender() != null) {
            user.setGender(resolveGender(request.getGender()));
        }
        if (StringUtils.hasText(request.getNationalIdNumber())) {
            user.setNationalIdNumber(request.getNationalIdNumber().trim());
        }
        if (StringUtils.hasText(request.getPassportNumber())) {
            user.setPassportNumber(request.getPassportNumber().trim());
        }
        if (request.getNationalIdUploadId() != null && request.getNationalIdUploadId() > 0) {
            user.setNationalIdUploadId(request.getNationalIdUploadId());
        }
        if (request.getPassportUploadId() != null && request.getPassportUploadId() > 0) {
            user.setPassportUploadId(request.getPassportUploadId());
        }
        if (StringUtils.hasText(request.getDateOfBirth())) {
            try {
                user.setDateOfBirth(java.sql.Date.valueOf(request.getDateOfBirth().trim()));
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid contact person date of birth for organisation {}: {}",
                        request.getOrganizationId(), request.getDateOfBirth());
            }
        }
    }

    private static boolean isFullyOnboardedContact(User user) {
        return user != null
                && Boolean.TRUE.equals(user.getEmailVerified())
                && !Boolean.TRUE.equals(user.getMustChangeCredentials());
    }

    /**
     * Organisation contact users created in a partial run may lack account/password rows required for portal login.
     */
    private UserResponse ensureLoginArtifacts(User user, Locale locale, String actor) {
        Long userId = user.getId();
        if (!hasActiveUserAccount(userId)) {
            CreateUserAccountRequest accountRequest = new CreateUserAccountRequest();
            accountRequest.setPhoneNumber(resolveAccountPhoneNumber(user));
            accountRequest.setIsAccountLocked(false);
            accountRequest.setUserId(userId);
            UserAccountResponse accountResponse = userAccountService.create(accountRequest, locale, actor);
            if (!accountResponse.isSuccess()) {
                return buildError(400, resolveErrorMessages(accountResponse.getErrorMessages(), accountResponse.getMessage()));
            }
        }
        if (!hasActiveUserPassword(userId)) {
            CreateUserPasswordRequest passwordRequest = new CreateUserPasswordRequest();
            passwordRequest.setUserId(userId);
            passwordRequest.setPassword(OrganizationContactCredentialsIssuer.generateCompliantPassword());
            var passwordResponse = userPasswordService.create(passwordRequest, locale, actor);
            if (!passwordResponse.isSuccess()) {
                return buildError(400, resolveErrorMessages(passwordResponse.getErrorMessages(), passwordResponse.getMessage()));
            }
        }
        if (!hasActiveUserSecurity(userId)) {
            UserSecurityResponse securityResponse =
                    attachSecurity(user, defaultSecurityDetails(), locale, actor);
            if (!securityResponse.isSuccess()) {
                return buildError(400, resolveErrorMessages(securityResponse.getErrorMessages(), securityResponse.getMessage()));
            }
        }
        return buildSuccess(user, locale, 200);
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

    /**
     * Account phone numbers are globally unique; org contacts often share the organisation phone on the user row.
     */
    private String resolveAccountPhoneNumber(User user) {
        String preferred = user.getPhoneNumber() != null ? user.getPhoneNumber().trim() : null;
        if (StringUtils.hasText(preferred) && isPhoneAvailableForUser(preferred, user.getId())) {
            return preferred;
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = provisioningAccountPhone(user.getId(), attempt);
            if (isPhoneAvailableForUser(candidate, user.getId())) {
                if (!candidate.equals(preferred)) {
                    log.info(
                            "Using dedicated account phone {} for contact user {} (shared user phone {})",
                            candidate,
                            user.getId(),
                            preferred);
                }
                return candidate;
            }
        }
        return provisioningAccountPhone(user.getId(), 0);
    }

    private static String provisioningAccountPhone(Long userId, int attempt) {
        long suffix = (userId != null ? userId : 0L) + (attempt * 1_000_000L);
        return "+263770" + String.format("%06d", suffix % 1_000_000L);
    }

    private boolean isPhoneAvailableForUser(String phone, Long userId) {
        if (!StringUtils.hasText(phone) || userId == null) {
            return false;
        }
        Optional<UserAccount> existing =
                userAccountRepository.findByPhoneNumberAndEntityStatusNot(phone.trim(), EntityStatus.DELETED);
        if (existing.isEmpty()) {
            return true;
        }
        User linkedUser = existing.get().getUser();
        return linkedUser != null && userId.equals(linkedUser.getId());
    }

    private static List<String> resolveErrorMessages(List<String> errorMessages, String message) {
        if (errorMessages != null && !errorMessages.isEmpty()) {
            return errorMessages;
        }
        if (StringUtils.hasText(message)) {
            return List.of(message);
        }
        return List.of("Request failed.");
    }

    private UserResponse buildSuccess(User user, Locale locale, int statusCode) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setOrganizationId(user.getOrganizationId());
        dto.setEmailVerified(user.getEmailVerified());
        UserResponse response = new UserResponse();
        response.setSuccess(true);
        response.setStatusCode(statusCode);
        response.setMessage(messageService.getMessage(I18Code.MESSAGE_USER_CREATED_SUCCESSFULLY.getCode(), new String[] {}, locale));
        response.setUserDto(dto);
        return response;
    }

    private static boolean shouldSendVerificationEmail(ProvisionOrganizationContactPersonRequest request) {
        return request == null || request.getSendVerificationEmail() == null || Boolean.TRUE.equals(request.getSendVerificationEmail());
    }

    private void sendContactPersonVerificationEmail(User user, ProvisionOrganizationContactPersonRequest request,
            String verificationToken) {
        try {
            boolean viaSignup = request.getViaSignup() == null || Boolean.TRUE.equals(request.getViaSignup());
            String portalBase = viaSignup ? organizationContactPortalBaseUrl : adminPortalBaseUrl;
            String verificationLink = emailVerificationLinkProperties.buildVerificationLink(
                    portalBase, verificationToken, user.getEmail());
            String signInLink = emailVerificationLinkProperties.buildSignInUrl(portalBase);

            Map<String, Object> templateExtras = new LinkedHashMap<>();
            templateExtras.put("organizationName", safe(request.getOrganizationName()));
            templateExtras.put("verificationLink", verificationLink);
            templateExtras.put("signInLink", signInLink);
            Map<String, Object> data = UserNotificationTemplateData.forUser(user, templateExtras);

            NotificationRequest notificationRequest = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    TEMPLATE_CONTACT_VERIFICATION,
                    new NotificationRequest.Recipient(user.getId().toString(), user.getEmail(), null, null),
                    data,
                    new NotificationRequest.Metadata("ldms-user-management", null));

            log.info("Publishing organisation contact person verification email to {}", user.getEmail());
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, notificationRequest);
        } catch (Exception e) {
            log.error("Failed to publish contact person verification email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    private UserType resolveOrganizationContactUserType(Locale locale, String actor) {
        return userTypeRepository
                .findByUserTypeNameAndEntityStatusNot(USER_TYPE_ORGANIZATION_CONTACT, EntityStatus.DELETED)
                .orElseGet(() -> {
                    CreateUserTypeRequest typeRequest = new CreateUserTypeRequest();
                    typeRequest.setUserTypeName(USER_TYPE_ORGANIZATION_CONTACT);
                    typeRequest.setDescription("Primary contact person for an organisation");
                    UserTypeResponse created = userTypeService.create(typeRequest, locale, actor);
                    if (created.getUserTypeDto() != null && created.getUserTypeDto().getId() != null) {
                        return userTypeRepository
                                .findByIdAndEntityStatusNot(created.getUserTypeDto().getId(), EntityStatus.DELETED)
                                .orElse(null);
                    }
                    return null;
                });
    }

    private UserSecurityResponse attachSecurity(User user, UserSecurityDetails details, Locale locale, String actor) {
        CreateUserSecurityRequest securityRequest = new CreateUserSecurityRequest();
        securityRequest.setUserId(user.getId());
        securityRequest.setSecurityQuestion_1(details.getSecurityQuestion_1());
        securityRequest.setSecurityQuestion_2(details.getSecurityQuestion_2());
        securityRequest.setSecurityAnswer_1(details.getSecurityAnswer_1());
        securityRequest.setSecurityAnswer_2(details.getSecurityAnswer_2());
        securityRequest.setTwoFactorAuthSecret(details.getTwoFactorAuthSecret());
        securityRequest.setIsTwoFactorEnabled(
                details.getIsTwoFactorEnabled() != null ? details.getIsTwoFactorEnabled() : Boolean.FALSE);
        return userSecurityService.create(securityRequest, locale, actor);
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

    private ValidatorDto validateProvisionRequest(ProvisionOrganizationContactPersonRequest request, Locale locale) {
        List<String> errors = new java.util.ArrayList<>();
        if (request == null) {
            errors.add("Request is required.");
            return new ValidatorDto(false, null, errors);
        }
        if (request.getOrganizationId() == null || request.getOrganizationId() < 1) {
            errors.add("Organisation id is required.");
        }
        if (!StringUtils.hasText(request.getOrganizationName())) {
            errors.add("Organisation name is required.");
        }
        if (!StringUtils.hasText(request.getEmail()) || !Validators.isValidEmail(request.getEmail().trim())) {
            errors.add("A valid contact person email is required.");
        }
        if (!StringUtils.hasText(request.getFirstName()) || !StringUtils.hasText(request.getLastName())) {
            errors.add("Contact person first and last name are required.");
        }
        String phone = resolvePhone(request);
        if (!Validators.isValidInternationalPhoneNumber(phone)) {
            errors.add("A valid contact phone number is required.");
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }

    private String resolvePhone(ProvisionOrganizationContactPersonRequest request) {
        if (StringUtils.hasText(request.getPhoneNumber())) {
            String trimmed = request.getPhoneNumber().trim();
            if (Validators.isValidInternationalPhoneNumber(trimmed)) {
                return trimmed;
            }
            String normalized = normalizeZimbabweLocalPhone(trimmed);
            if (Validators.isValidInternationalPhoneNumber(normalized)) {
                return normalized;
            }
            log.warn("Contact person phone '{}' is not E.164; using default placeholder for provisioning", trimmed);
        }
        return "+263770000001";
    }

    private static String normalizeZimbabweLocalPhone(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("263") && digits.length() >= 12) {
            return "+" + digits;
        }
        if (digits.startsWith("0") && digits.length() >= 9) {
            return "+263" + digits.substring(1);
        }
        return raw;
    }

    private static Gender resolveGender(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Gender.PREFER_NOT_TO_SAY;
        }
        try {
            return Gender.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Gender.PREFER_NOT_TO_SAY;
        }
    }

    private static String resolveNationalId(ProvisionOrganizationContactPersonRequest request) {
        if (StringUtils.hasText(request.getNationalIdNumber())) {
            return request.getNationalIdNumber().trim();
        }
        return "PENDING-" + request.getOrganizationId();
    }

    private static UserResponse buildError(int status, List<String> errors) {
        UserResponse response = new UserResponse();
        response.setSuccess(false);
        response.setStatusCode(status);
        response.setMessage(errors != null && !errors.isEmpty() ? errors.get(0) : "Request failed.");
        response.setErrorMessages(errors);
        return response;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
