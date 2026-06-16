package projectlx.co.zw.shared_library.utils.notifications;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.requests.NotificationRequest;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sends logistics lifecycle notifications (shipment allocated, trip started, trip completed)
 * to the organisation's contacts, fleet managers, and the assigned driver via
 * {@code notifications.direct} / {@code notifications.send}.
 *
 * <p>Callers must pre-resolve recipient data (org contacts, fleet managers list, driver
 * contact) and pass it as parameters.  This class only handles notification assembly and
 * publish — it never throws on failure (logs and continues).</p>
 */
@Component
@RequiredArgsConstructor
public class LogisticsLifecycleNotificationSupport {

    private static final Logger log = LoggerFactory.getLogger(LogisticsLifecycleNotificationSupport.class);

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";

    private static final String TEMPLATE_SHIPMENT_ALLOCATED = "SHIPMENT_ALLOCATED";
    private static final String TEMPLATE_TRIP_STARTED = "TRIP_STARTED";
    private static final String TEMPLATE_TRIP_COMPLETED = "TRIP_COMPLETED";

    private final RabbitTemplate rabbitTemplate;

    @Value("${ldms.portal.base-url:http://localhost:4201}")
    private String portalBaseUrl;

    // ============================================================
    // Public notification methods
    // ============================================================

    /**
     * Notify all stakeholders when a shipment is allocated to a fleet driver and asset.
     *
     * @param orgId               organisation ID (used as recipient namespace)
     * @param orgEmail            organisation inbox email (may be null)
     * @param orgPhone            organisation phone (may be null)
     * @param contactEmail        contact person email (may be null; deduped vs orgEmail)
     * @param contactPhone        contact person phone (may be null)
     * @param orgName             organisation display name
     * @param contactName         contact person display name
     * @param fleetManagers       list of users with fleet-manager roles in the org workspace
     * @param driverEmail         resolved driver email (may be null)
     * @param driverPhone         resolved driver phone (may be null)
     * @param driverName          resolved driver full name
     * @param shipmentNumber      shipment reference number
     * @param fromWarehouse       origin warehouse name
     * @param toWarehouse         destination warehouse name
     * @param productName         product being shipped
     * @param quantity            quantity as string
     * @param performedBy         username of the actor who performed the allocation
     */
    public void notifyShipmentAllocated(
            Long orgId,
            String orgEmail, String orgPhone,
            String contactEmail, String contactPhone,
            String orgName, String contactName,
            List<UserDto> fleetManagers,
            String driverEmail, String driverPhone, String driverName,
            String shipmentNumber, String fromWarehouse, String toWarehouse,
            String productName, String quantity, String performedBy) {

        Map<String, Object> baseData = buildBaseData(orgName, contactName, performedBy);
        baseData.put("shipmentNumber", safe(shipmentNumber));
        baseData.put("fromWarehouse", safe(fromWarehouse));
        baseData.put("toWarehouse", safe(toWarehouse));
        baseData.put("productName", safe(productName));
        baseData.put("quantity", safe(quantity));
        baseData.put("tripNumber", "");
        baseData.put("lifecycleMessage", "Shipment " + safe(shipmentNumber) + " has been allocated to a driver and is ready for dispatch.");

        sendToOrgAndManagers(orgId, orgEmail, orgPhone, contactEmail, contactPhone,
                fleetManagers, TEMPLATE_SHIPMENT_ALLOCATED, baseData);
        sendToDriver(orgId, driverEmail, driverPhone, driverName, TEMPLATE_SHIPMENT_ALLOCATED, baseData);
    }

    /**
     * Notify all stakeholders when a trip starts.
     */
    public void notifyTripStarted(
            Long orgId,
            String orgEmail, String orgPhone,
            String contactEmail, String contactPhone,
            String orgName, String contactName,
            List<UserDto> fleetManagers,
            String driverEmail, String driverPhone, String driverName,
            String tripNumber, String shipmentNumber,
            String fromWarehouse, String toWarehouse,
            String productName, String performedBy) {

        Map<String, Object> baseData = buildBaseData(orgName, contactName, performedBy);
        baseData.put("tripNumber", safe(tripNumber));
        baseData.put("shipmentNumber", safe(shipmentNumber));
        baseData.put("fromWarehouse", safe(fromWarehouse));
        baseData.put("toWarehouse", safe(toWarehouse));
        baseData.put("productName", safe(productName));
        baseData.put("quantity", "");
        baseData.put("lifecycleMessage", "Trip " + safe(tripNumber) + " has started. Driver is en route from "
                + safe(fromWarehouse) + " to " + safe(toWarehouse) + ".");

        sendToOrgAndManagers(orgId, orgEmail, orgPhone, contactEmail, contactPhone,
                fleetManagers, TEMPLATE_TRIP_STARTED, baseData);
        sendToDriver(orgId, driverEmail, driverPhone, driverName, TEMPLATE_TRIP_STARTED, baseData);
    }

    /**
     * Notify all stakeholders when a trip is completed (delivery confirmed via OTP).
     */
    public void notifyTripCompleted(
            Long orgId,
            String orgEmail, String orgPhone,
            String contactEmail, String contactPhone,
            String orgName, String contactName,
            List<UserDto> fleetManagers,
            String driverEmail, String driverPhone, String driverName,
            String tripNumber, String shipmentNumber,
            String fromWarehouse, String toWarehouse,
            String productName, String performedBy) {

        Map<String, Object> baseData = buildBaseData(orgName, contactName, performedBy);
        baseData.put("tripNumber", safe(tripNumber));
        baseData.put("shipmentNumber", safe(shipmentNumber));
        baseData.put("fromWarehouse", safe(fromWarehouse));
        baseData.put("toWarehouse", safe(toWarehouse));
        baseData.put("productName", safe(productName));
        baseData.put("quantity", "");
        baseData.put("lifecycleMessage", "Trip " + safe(tripNumber) + " has been completed. Delivery to "
                + safe(toWarehouse) + " was confirmed.");

        sendToOrgAndManagers(orgId, orgEmail, orgPhone, contactEmail, contactPhone,
                fleetManagers, TEMPLATE_TRIP_COMPLETED, baseData);
        sendToDriver(orgId, driverEmail, driverPhone, driverName, TEMPLATE_TRIP_COMPLETED, baseData);
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private Map<String, Object> buildBaseData(String orgName, String contactName, String performedBy) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("organizationName", safe(orgName));
        data.put("contactName", safe(contactName));
        data.put("performedBy", safe(performedBy));
        data.put("signInLink", portalBaseUrl);
        data.put("driverName", "");
        data.put("recipientRole", "stakeholder");
        return data;
    }

    private void sendToOrgAndManagers(
            Long orgId,
            String orgEmail, String orgPhone,
            String contactEmail, String contactPhone,
            List<UserDto> fleetManagers,
            String templateKey,
            Map<String, Object> baseData) {

        Set<String> sentEmails = new LinkedHashSet<>();

        String normalizedOrgEmail = normalizeEmail(orgEmail);
        if (StringUtils.hasText(normalizedOrgEmail)) {
            publishToEmail(orgId, normalizedOrgEmail, safe(orgPhone), templateKey, baseData, "organization", sentEmails);
        }

        String normalizedContactEmail = normalizeEmail(contactEmail);
        if (StringUtils.hasText(normalizedContactEmail) && !sentEmails.contains(normalizedContactEmail)) {
            publishToEmail(orgId, normalizedContactEmail, safe(contactPhone), templateKey, baseData, "contact-person", sentEmails);
        }

        if (fleetManagers != null) {
            for (UserDto manager : fleetManagers) {
                String managerEmail = normalizeEmail(manager.getEmail());
                String managerPhone = safe(manager.getPhoneNumber());
                String managerName = buildDisplayName(manager);
                if (StringUtils.hasText(managerEmail) && !sentEmails.contains(managerEmail)) {
                    Map<String, Object> managerData = new LinkedHashMap<>(baseData);
                    managerData.put("contactName", managerName);
                    managerData.put("recipientRole", "fleet-manager");
                    publishToEmail(orgId, managerEmail, managerPhone, templateKey, managerData, "fleet-manager", sentEmails);
                }
            }
        }
    }

    private void sendToDriver(
            Long orgId,
            String driverEmail, String driverPhone, String driverName,
            String templateKey,
            Map<String, Object> baseData) {

        String normalizedEmail = normalizeEmail(driverEmail);
        boolean hasEmail = StringUtils.hasText(normalizedEmail);
        boolean hasPhone = StringUtils.hasText(driverPhone);

        if (!hasEmail && !hasPhone) {
            return;
        }

        Map<String, Object> driverData = new LinkedHashMap<>(baseData);
        driverData.put("contactName", safe(driverName));
        driverData.put("driverName", safe(driverName));
        driverData.put("recipientRole", "driver");

        if (hasEmail) {
            driverData.put("email", normalizedEmail);
            driverData.put("Email", normalizedEmail);
            driverData.put("phoneNumber", safe(driverPhone));
            String recipientUserId = orgId != null ? orgId + ":driver" : "driver";
            NotificationRequest request = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    templateKey,
                    new NotificationRequest.Recipient(recipientUserId, normalizedEmail, safe(driverPhone), null),
                    driverData,
                    new NotificationRequest.Metadata("ldms-logistics", null));
            publish(request, templateKey, "driver", normalizedEmail);
        } else {
            driverData.put("email", "");
            driverData.put("Email", "");
            driverData.put("phoneNumber", safe(driverPhone));
            String recipientUserId = orgId != null ? orgId + ":driver-sms" : "driver-sms";
            NotificationRequest request = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    templateKey,
                    new NotificationRequest.Recipient(recipientUserId, null, safe(driverPhone), null),
                    driverData,
                    new NotificationRequest.Metadata("ldms-logistics", null));
            publish(request, templateKey, "driver-sms", driverPhone);
        }
    }

    private void publishToEmail(
            Long orgId,
            String email, String phone,
            String templateKey,
            Map<String, Object> baseData,
            String recipientRole,
            Set<String> sentEmails) {

        Map<String, Object> data = new LinkedHashMap<>(baseData);
        data.put("email", email);
        data.put("Email", email);
        data.put("phoneNumber", safe(phone));

        String recipientUserId = orgId != null ? orgId + ":" + recipientRole : recipientRole;
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID().toString(),
                templateKey,
                new NotificationRequest.Recipient(recipientUserId, email, safe(phone), null),
                data,
                new NotificationRequest.Metadata("ldms-logistics", null));
        publish(request, templateKey, recipientRole, email);
        sentEmails.add(email);
    }

    private void publish(NotificationRequest request, String templateKey, String role, String to) {
        try {
            log.info("Publishing logistics notification template={} role={} to={}", templateKey, role, to);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        } catch (Exception ex) {
            log.error("Failed to publish logistics notification template={} role={} to={}: {}",
                    templateKey, role, to, ex.getMessage());
        }
    }

    private static String buildDisplayName(UserDto user) {
        if (user == null) return "";
        String first = safe(user.getFirstName());
        String last = safe(user.getLastName());
        String name = (first + " " + last).trim();
        return StringUtils.hasText(name) ? name : safe(user.getUsername());
    }

    private static String normalizeEmail(String email) {
        if (email == null) return "";
        String trimmed = email.trim().toLowerCase();
        return trimmed.contains("@") ? trimmed : "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
