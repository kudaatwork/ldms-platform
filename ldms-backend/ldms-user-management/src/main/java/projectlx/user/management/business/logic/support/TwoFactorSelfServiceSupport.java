package projectlx.user.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.enums.TwoFactorMethod;
import projectlx.co.zw.shared_library.utils.security.TotpSupport;
import projectlx.user.management.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.OtpType;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserSecurity;
import projectlx.user.management.repository.UserSecurityRepository;
import projectlx.user.management.utils.dtos.UserSecurityDto;
import projectlx.user.management.utils.requests.TwoFactorOtpRequest;

import java.util.Optional;
import java.util.UUID;

/**
 * Self-service two-factor enrollment for the signed-in user (My Account).
 */
@RequiredArgsConstructor
public class TwoFactorSelfServiceSupport {

    private static final String ISSUER = "Project LX LDMS";

    private final UserSecurityRepository userSecurityRepository;
    private final UserSecurityServiceAuditable userSecurityServiceAuditable;
    private final PhoneVerificationSupport phoneVerificationSupport;

    public Optional<UserSecurity> findActiveSecurity(User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        return userSecurityRepository.findByUser_IdAndEntityStatusNot(user.getId(), EntityStatus.DELETED);
    }

    public UserSecurity ensureSecurityRecord(User user, String actor) {
        Optional<UserSecurity> existing = findActiveSecurity(user);
        if (existing.isPresent()) {
            return existing.get();
        }
        UserSecurity created = new UserSecurity();
        created.setUser(user);
        created.setSecurityQuestion_1("Please set your first security question (profile).");
        created.setSecurityAnswer_1("TEMP-A1-" + UUID.randomUUID());
        created.setSecurityQuestion_2("Please set your second security question (profile).");
        created.setSecurityAnswer_2("TEMP-A2-" + UUID.randomUUID());
        created.setTwoFactorAuthSecret(TotpSupport.generateSecret());
        created.setIsTwoFactorEnabled(Boolean.FALSE);
        created.setTwoFactorMethod(null);
        return userSecurityServiceAuditable.create(created, java.util.Locale.getDefault(), actor);
    }

    public BeginAuthenticatorSetup beginAuthenticatorSetup(User user, String actor) {
        UserSecurity security = ensureSecurityRecord(user, actor);
        if (Boolean.TRUE.equals(security.getIsTwoFactorEnabled())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.ALREADY_ENABLED);
        }
        String secret = TotpSupport.generateSecret();
        security.setTwoFactorAuthSecret(secret);
        security.setIsTwoFactorEnabled(Boolean.FALSE);
        security.setTwoFactorMethod(null);
        userSecurityServiceAuditable.update(security, java.util.Locale.getDefault(), actor);

        String account = resolveAccountLabel(user);
        var qrData = TotpSupport.buildQrData(secret, account, ISSUER);
        String otpAuthUri = qrData.getUri();
        return new BeginAuthenticatorSetup(secret, otpAuthUri, TotpSupport.qrCodeDataUrl(qrData));
    }

    public UserSecurity confirmAuthenticatorSetup(User user, TwoFactorOtpRequest request, String actor) {
        UserSecurity security = requireSecurity(user);
        if (Boolean.TRUE.equals(security.getIsTwoFactorEnabled())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.ALREADY_ENABLED);
        }
        if (!StringUtils.hasText(security.getTwoFactorAuthSecret())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.SETUP_NOT_STARTED);
        }
        if (!TotpSupport.verifyCode(security.getTwoFactorAuthSecret(), request.getOtp())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.OTP_INVALID);
        }
        security.setIsTwoFactorEnabled(Boolean.TRUE);
        security.setTwoFactorMethod(TwoFactorMethod.AUTHENTICATOR_APP);
        return userSecurityServiceAuditable.update(security, java.util.Locale.getDefault(), actor);
    }

    public UserSecurity enableSmsTwoFactor(User user, String actor) {
        if (!StringUtils.hasText(user.getPhoneNumber())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.PHONE_MISSING);
        }
        if (!Boolean.TRUE.equals(user.getPhoneVerified())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.PHONE_NOT_VERIFIED);
        }
        UserSecurity security = ensureSecurityRecord(user, actor);
        if (Boolean.TRUE.equals(security.getIsTwoFactorEnabled())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.ALREADY_ENABLED);
        }
        if (!StringUtils.hasText(security.getTwoFactorAuthSecret())) {
            security.setTwoFactorAuthSecret(TotpSupport.generateSecret());
        }
        security.setIsTwoFactorEnabled(Boolean.TRUE);
        security.setTwoFactorMethod(TwoFactorMethod.SMS);
        return userSecurityServiceAuditable.update(security, java.util.Locale.getDefault(), actor);
    }

    public UserSecurity disableTwoFactor(User user, TwoFactorOtpRequest request, String actor) {
        UserSecurity security = requireSecurity(user);
        if (!Boolean.TRUE.equals(security.getIsTwoFactorEnabled())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.NOT_ENABLED);
        }
        TwoFactorMethod method = resolveMethod(security);
        if (!verifyDisableOtp(user, security, method, request.getOtp())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.OTP_INVALID);
        }
        security.setIsTwoFactorEnabled(Boolean.FALSE);
        security.setTwoFactorMethod(null);
        return userSecurityServiceAuditable.update(security, java.util.Locale.getDefault(), actor);
    }

    /**
     * Admin override: disables 2FA without verifying an end-user OTP (support / user-management request).
     */
    public UserSecurity disableTwoFactorAsAdmin(User user, String actor) {
        UserSecurity security = requireSecurity(user);
        if (!Boolean.TRUE.equals(security.getIsTwoFactorEnabled())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.NOT_ENABLED);
        }
        security.setIsTwoFactorEnabled(Boolean.FALSE);
        security.setTwoFactorMethod(null);
        return userSecurityServiceAuditable.update(security, java.util.Locale.getDefault(), actor);
    }

    public UserSecurityDto toAdminReadDto(UserSecurity security) {
        UserSecurityDto dto = sanitizeForSelfServiceRead(security);
        dto.setId(security.getId());
        return dto;
    }

    public void sendSmsVerificationForDisable(User user, String actor) {
        UserSecurity security = requireSecurity(user);
        if (!Boolean.TRUE.equals(security.getIsTwoFactorEnabled())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.NOT_ENABLED);
        }
        if (resolveMethod(security) != TwoFactorMethod.SMS) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.WRONG_METHOD);
        }
        if (!StringUtils.hasText(user.getPhoneNumber())) {
            throw new TwoFactorSelfServiceException(TwoFactorSelfServiceError.PHONE_MISSING);
        }
        phoneVerificationSupport.generateAndSendOtp(user, OtpType.STEP_UP, actor);
    }

    public UserSecurityDto sanitizeForSelfServiceRead(UserSecurity security) {
        UserSecurityDto dto = new UserSecurityDto();
        dto.setId(security.getId());
        dto.setSecurityQuestion_1(security.getSecurityQuestion_1());
        dto.setSecurityQuestion_2(security.getSecurityQuestion_2());
        dto.setIsTwoFactorEnabled(security.getIsTwoFactorEnabled());
        dto.setTwoFactorMethod(security.getTwoFactorMethod());
        return dto;
    }

    private UserSecurity requireSecurity(User user) {
        return findActiveSecurity(user)
                .orElseThrow(() -> new TwoFactorSelfServiceException(TwoFactorSelfServiceError.SECURITY_NOT_FOUND));
    }

    private boolean verifyDisableOtp(User user, UserSecurity security, TwoFactorMethod method, String otp) {
        if (method == TwoFactorMethod.AUTHENTICATOR_APP) {
            return TotpSupport.verifyCode(security.getTwoFactorAuthSecret(), otp);
        }
        return phoneVerificationSupport.verifyOtp(user.getId(), otp, OtpType.STEP_UP);
    }

    public static TwoFactorMethod resolveMethod(UserSecurity security) {
        if (security == null) {
            return TwoFactorMethod.SMS;
        }
        if (security.getTwoFactorMethod() != null) {
            return security.getTwoFactorMethod();
        }
        return TwoFactorMethod.SMS;
    }

    private static String resolveAccountLabel(User user) {
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "user-" + user.getId();
    }

    public record BeginAuthenticatorSetup(String secret, String otpAuthUri, String qrCodeDataUrl) {
    }

    public enum TwoFactorSelfServiceError {
        ALREADY_ENABLED,
        NOT_ENABLED,
        SETUP_NOT_STARTED,
        OTP_INVALID,
        PHONE_MISSING,
        PHONE_NOT_VERIFIED,
        SECURITY_NOT_FOUND,
        WRONG_METHOD
    }

    public static class TwoFactorSelfServiceException extends RuntimeException {
        private final TwoFactorSelfServiceError error;

        public TwoFactorSelfServiceException(TwoFactorSelfServiceError error) {
            super(error.name());
            this.error = error;
        }

        public TwoFactorSelfServiceError getError() {
            return error;
        }
    }
}
