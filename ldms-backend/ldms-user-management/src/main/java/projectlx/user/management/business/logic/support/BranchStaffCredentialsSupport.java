package projectlx.user.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.user.management.model.User;
import projectlx.user.management.utils.config.EmailVerificationLinkProperties;
import projectlx.user.management.utils.notifications.UserNotificationTemplateData;
import projectlx.user.management.utils.requests.NotificationRequest;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

/**
 * Issues temporary portal credentials for branch staff (branch clerks and managers)
 * created from the workforce dialogs, and emails them a sign-in link.
 *
 * <p>Mirrors the driver / organisation-contact onboarding flow: the platform generates
 * a temporary username and a compliant temporary password (must-change on first login)
 * and dispatches a {@code BRANCH_STAFF_APPROVED_CREDENTIALS} notification. On first
 * sign-in the user is forced through {@code /auth/setup-credentials} to choose permanent
 * credentials.</p>
 */
@Component
@RequiredArgsConstructor
public class BranchStaffCredentialsSupport {

    private static final Logger log = LoggerFactory.getLogger(BranchStaffCredentialsSupport.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_USERNAME_ATTEMPTS = 12;

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";
    private static final String TEMPLATE_BRANCH_STAFF_CREDENTIALS = "BRANCH_STAFF_APPROVED_CREDENTIALS";

    private final UsernameUniquenessSupport usernameUniquenessSupport;
    private final EmailVerificationLinkProperties emailVerificationLinkProperties;
    private final RabbitTemplate rabbitTemplate;

    /** Generates a globally-unique temporary username for a not-yet-created user. */
    public String generateUniqueTemporaryUsername(Long organizationId) {
        for (int attempt = 0; attempt < MAX_USERNAME_ATTEMPTS; attempt++) {
            String candidate = generateTemporaryUsername(organizationId);
            if (usernameUniquenessSupport.isAvailable(candidate, null)) {
                return candidate;
            }
        }
        return generateTemporaryUsername(organizationId)
                + Integer.toString(SECURE_RANDOM.nextInt(900_000) + 100_000, 36);
    }

    /** Generates a compliant temporary password (reuses the shared generator). */
    public String generateTemporaryPassword() {
        return OrganizationContactCredentialsIssuer.generateCompliantPassword();
    }

    /** Publishes the temporary-credentials email with a sign-in link. Best-effort. */
    public void publishCredentialsEmail(User user, String temporaryUsername, String temporaryPassword) {
        if (user == null || !StringUtils.hasText(user.getEmail())
                || !StringUtils.hasText(temporaryUsername) || !StringUtils.hasText(temporaryPassword)) {
            log.warn("Skipping branch staff credentials email: missing user email or credentials");
            return;
        }

        String signInLink = emailVerificationLinkProperties.buildSignInUrl();

        Map<String, Object> data = UserNotificationTemplateData.forUser(user, Map.of(
                "temporaryUsername", temporaryUsername,
                "temporaryPassword", temporaryPassword,
                "signInLink", signInLink
        ));

        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                user.getId() != null ? user.getId().toString() : null,
                user.getEmail(),
                null,
                null
        );

        NotificationRequest notificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                TEMPLATE_BRANCH_STAFF_CREDENTIALS,
                recipient,
                data,
                new NotificationRequest.Metadata("ldms-user-management", null)
        );

        try {
            log.info("Publishing branch staff credentials email for user: {}", user.getEmail());
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, notificationRequest);
            log.info("Successfully published branch staff credentials email for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish branch staff credentials email for user: {}. Error: {}",
                    user.getEmail(), e.getMessage());
        }
    }

    private static String generateTemporaryUsername(Long organizationId) {
        String suffix = Integer.toString(SECURE_RANDOM.nextInt(900_000) + 100_000, 36);
        long orgPart = organizationId != null ? organizationId : 0L;
        return "lx" + orgPart + "s" + suffix;
    }
}
