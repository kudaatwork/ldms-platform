package projectlx.fleet.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.fleet.management.clients.UserManagementServiceClient;
import projectlx.fleet.management.clients.dto.ProvisionDriverPlatformUserRequest;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.fleet.management.utils.requests.NotificationRequest;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Provisions a fleet driver as a platform user and dispatches credentials email.
 *
 * <p>Flow:
 * <ol>
 *   <li>Build {@link ProvisionDriverPlatformUserRequest} from driver profile.</li>
 *   <li>Call ldms-user-management to create user + issue temporary credentials.</li>
 *   <li>Publish {@code DRIVER_APPROVED_CREDENTIALS} notification event.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FleetDriverOnboardingSupport {

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";
    private static final String TEMPLATE_DRIVER_CREDENTIALS = "DRIVER_APPROVED_CREDENTIALS";

    private final UserManagementServiceClient userManagementServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${ldms.fleet.driver-portal-base-url:http://localhost:4202}")
    private String driverPortalBaseUrl;

    /**
     * Provisions the driver user account and publishes the credentials email.
     *
     * @param driver   the saved FleetDriver entity (must have organizationId, firstName, lastName set)
     * @param email    driver email address
     * @param locale   request locale
     * @param actor    username of the caller
     * @return the user-management UserResponse (contains userId + temporary credentials)
     */
    public UserResponse provisionAndNotify(FleetDriver driver, String email, Locale locale, String actor) {

        // ============================================================
        // STEP 1: Call user-management to provision user
        // ============================================================
        ProvisionDriverPlatformUserRequest request = new ProvisionDriverPlatformUserRequest();
        request.setOrganizationId(driver.getOrganizationId());
        request.setFirstName(driver.getFirstName());
        request.setLastName(driver.getLastName());
        request.setEmail(email);
        request.setPhoneNumber(driver.getPhoneNumber());
        request.setLicenseNumber(driver.getLicenseNumber());
        if (driver.getUserId() != null && driver.getUserId() > 0) {
            request.setExistingUserId(driver.getUserId());
        }

        UserResponse userResponse;
        try {
            userResponse = userManagementServiceClient.provisionDriverPlatformAccess(request);
        } catch (Exception ex) {
            log.error("Failed to provision driver platform user for driverId={} email={}: {}",
                    driver.getId(), email, ex.getMessage(), ex);
            UserResponse err = new UserResponse();
            err.setSuccess(false);
            err.setStatusCode(500);
            err.setMessage("Failed to provision driver platform user: " + ex.getMessage());
            return err;
        }

        if (!userResponse.isSuccess()) {
            log.warn("User-management returned failure for driver={}: {}", driver.getId(), userResponse.getMessage());
            return userResponse;
        }

        // ============================================================
        // STEP 2: Publish credentials notification
        // ============================================================
        String temporaryUsername = userResponse.getTemporaryUsername();
        String temporaryPassword = userResponse.getTemporaryPassword();
        if (temporaryUsername != null && temporaryPassword != null) {
            publishDriverCredentialsEmail(driver, email, temporaryUsername, temporaryPassword);
        }

        return userResponse;
    }

    // ============================================================
    // Notification helper
    // ============================================================

    private void publishDriverCredentialsEmail(FleetDriver driver, String email,
            String temporaryUsername, String temporaryPassword) {
        try {
            String signInLink = driverPortalBaseUrl + "/auth/login?portal=driver";

            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("firstName", driver.getFirstName());
            variables.put("temporaryUsername", temporaryUsername);
            variables.put("temporaryPassword", temporaryPassword);
            variables.put("signInLink", signInLink);

            NotificationRequest notificationRequest = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    TEMPLATE_DRIVER_CREDENTIALS,
                    new NotificationRequest.Recipient(
                            driver.getUserId() != null ? driver.getUserId().toString() : "0",
                            email,
                            null,
                            null),
                    variables,
                    new NotificationRequest.Metadata("ldms-fleet-management", null));

            log.info("Publishing driver credentials email to {}", email);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, notificationRequest);
        } catch (Exception ex) {
            log.error("Failed to publish driver credentials email to {}: {}", email, ex.getMessage(), ex);
        }
    }
}
