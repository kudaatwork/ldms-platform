package projectlx.user.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.OtpType;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserOtpChallenge;
import projectlx.user.management.repository.UserOtpChallengeRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.utils.config.PhoneVerificationProperties;
import projectlx.user.management.utils.notifications.UserNotificationTemplateData;
import projectlx.user.management.utils.requests.NotificationRequest;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates OTP generation, delivery, and verification for:
 * <ul>
 *   <li>Phone number verification ({@link OtpType#PHONE_VERIFICATION})</li>
 *   <li>Login two-factor challenge ({@link OtpType#LOGIN_2FA})</li>
 *   <li>One-time step-up authentication ({@link OtpType#STEP_UP})</li>
 * </ul>
 *
 * <p>OTPs are 6 digits, BCrypt-hashed before persistence, and expire in 10 minutes.
 * A user's phone is flagged as {@code phoneVerified=true} and
 * {@code lastPhoneVerifiedAt} is stamped only when a {@code PHONE_VERIFICATION} OTP
 * is confirmed successfully.</p>
 *
 * <p>The {@code phoneVerificationDue} flag (computed — never stored) is {@code true}
 * when a user has a phone number that has not been verified ({@code phoneVerified=false})
 * and the account is at least 14 days old.</p>
 */
@RequiredArgsConstructor
public class PhoneVerificationSupport {

    public static final String SMS_DELIVERY_DISABLED_CODE = "SMS_DELIVERY_DISABLED";

    private static final Logger logger = LoggerFactory.getLogger(PhoneVerificationSupport.class);

    private static final int OTP_DIGITS = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int PHONE_VERIFICATION_DUE_DAYS = 14;

    private static final String NOTIFICATION_EXCHANGE   = "notifications.direct";
    private static final String NOTIFICATION_ROUTING_KEY = "notifications.send";

    private final UserOtpChallengeRepository otpChallengeRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private final PhoneVerificationProperties phoneVerificationProperties;

    // ============================================================
    //  OTP generation & delivery
    // ============================================================

    /**
     * Generates a 6-digit OTP for the given user and type, invalidates any previous
     * pending OTPs of the same type, persists the BCrypt hash, and publishes an SMS
     * notification via RabbitMQ.
     *
     * @param user     the user who will receive the OTP
     * @param otpType  the purpose of this OTP
     * @param actor    the username of the initiating principal (for audit)
     * @return the plain-text OTP (only in-memory, never persisted)
     */
    public boolean isSmsDeliveryEnabled() {
        return phoneVerificationProperties.isSmsEnabled();
    }

    public String generateAndSendOtp(User user, OtpType otpType, String actor) {
        if (!phoneVerificationProperties.isSmsEnabled()) {
            throw new SmsDeliveryDisabledException(SMS_DELIVERY_DISABLED_CODE);
        }

        // ============================================================
        // STEP 1: Invalidate any existing pending OTPs for this user+type
        // ============================================================
        otpChallengeRepository.invalidatePreviousChallenges(user.getId(), otpType);

        // ============================================================
        // STEP 2: Generate a 6-digit OTP
        // ============================================================
        String plainOtp = generateSixDigitOtp();
        String otpHash  = passwordEncoder.encode(plainOtp);

        // ============================================================
        // STEP 3: Persist the OTP challenge record
        // ============================================================
        UserOtpChallenge challenge = new UserOtpChallenge();
        challenge.setUserId(user.getId());
        challenge.setOtpType(otpType);
        challenge.setOtpHash(otpHash);
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        challenge.setUsed(false);
        challenge.setEntityStatus(EntityStatus.ACTIVE);
        challenge.setCreatedAt(LocalDateTime.now());
        challenge.setCreatedBy(actor);
        otpChallengeRepository.save(challenge);

        // ============================================================
        // STEP 4: Publish SMS via RabbitMQ
        // ============================================================
        String templateKey = resolveTemplateKey(otpType);
        Map<String, Object> templateData = UserNotificationTemplateData.forUser(user, Map.of("otp", plainOtp));
        NotificationRequest notification = buildNotificationRequest(user, templateKey, templateData);

        try {
            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, NOTIFICATION_ROUTING_KEY, notification);
            logger.info("OTP notification published for userId={} type={}", user.getId(), otpType);
        } catch (Exception ex) {
            logger.error("Failed to publish OTP notification for userId={} type={}", user.getId(), otpType, ex);
            throw new SmsDeliveryDisabledException(SMS_DELIVERY_DISABLED_CODE);
        }

        return plainOtp;
    }

    // ============================================================
    //  OTP verification
    // ============================================================

    /**
     * Verifies a plain-text OTP against the most recent active, unused, unexpired
     * challenge for the given user and type.
     *
     * @param userId   the user's ID
     * @param plainOtp the OTP submitted by the user
     * @param otpType  the purpose being verified
     * @return {@code true} if verification succeeds; {@code false} otherwise
     */
    public boolean verifyOtp(Long userId, String plainOtp, OtpType otpType) {

        if (!StringUtils.hasText(plainOtp) || userId == null) {
            return false;
        }

        Optional<UserOtpChallenge> challengeOpt =
                otpChallengeRepository
                        .findTopByUserIdAndOtpTypeAndUsedFalseAndExpiresAtAfterAndEntityStatusNotOrderByCreatedAtDesc(
                                userId, otpType, LocalDateTime.now(), EntityStatus.DELETED);

        if (challengeOpt.isEmpty()) {
            logger.warn("No active OTP challenge found for userId={} type={}", userId, otpType);
            return false;
        }

        UserOtpChallenge challenge = challengeOpt.get();

        if (!passwordEncoder.matches(plainOtp, challenge.getOtpHash())) {
            logger.warn("OTP mismatch for userId={} type={}", userId, otpType);
            return false;
        }

        // Consume the challenge
        challenge.setUsed(true);
        otpChallengeRepository.save(challenge);
        logger.info("OTP verified successfully for userId={} type={}", userId, otpType);
        return true;
    }

    // ============================================================
    //  Phone verification commit
    // ============================================================

    /**
     * Stamps {@code phoneVerified=true} and {@code lastPhoneVerifiedAt=now()} on the
     * user entity after successful OTP confirmation.
     *
     * @param user the user whose phone was just verified
     */
    public void markPhoneVerified(User user) {
        user.setPhoneVerified(true);
        user.setLastPhoneVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Phone marked as verified for userId={}", user.getId());
    }

    // ============================================================
    //  phoneVerificationDue computation (read-only / computed)
    // ============================================================

    /**
     * Returns {@code true} when the user has a phone number, has not verified it yet,
     * and their account is at least {@value #PHONE_VERIFICATION_DUE_DAYS} days old.
     *
     * @param user the user to evaluate
     * @return whether phone verification is overdue
     */
    public boolean isPhoneVerificationDue(User user) {
        if (!StringUtils.hasText(user.getPhoneNumber())) {
            return false;
        }
        LocalDateTime anchor = user.getLastPhoneVerifiedAt() != null
                ? user.getLastPhoneVerifiedAt()
                : user.getCreatedAt();
        if (anchor == null) {
            return true;
        }
        return anchor.plusDays(PHONE_VERIFICATION_DUE_DAYS).isBefore(LocalDateTime.now());
    }

    // ============================================================
    //  Private helpers
    // ============================================================

    private String generateSixDigitOtp() {
        SecureRandom rng = new SecureRandom();
        int value = 100_000 + rng.nextInt(900_000);
        return String.valueOf(value);
    }

    private String resolveTemplateKey(OtpType otpType) {
        return switch (otpType) {
            case PHONE_VERIFICATION -> "PHONE_VERIFICATION_OTP";
            case LOGIN_2FA          -> "LOGIN_2FA_OTP";
            case STEP_UP            -> "PHONE_VERIFICATION_OTP";
        };
    }

    private NotificationRequest buildNotificationRequest(User user, String templateKey,
                                                          Map<String, Object> data) {
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                user.getId() != null ? String.valueOf(user.getId()) : null,
                null,
                user.getPhoneNumber(),
                null
        );
        NotificationRequest.Metadata metadata = new NotificationRequest.Metadata(
                "ldms-user-management",
                UUID.randomUUID().toString()
        );
        return new NotificationRequest(UUID.randomUUID().toString(), templateKey, recipient, data, metadata);
    }
}
