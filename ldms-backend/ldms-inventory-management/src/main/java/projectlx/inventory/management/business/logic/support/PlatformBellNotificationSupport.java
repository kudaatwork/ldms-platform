package projectlx.inventory.management.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.notifications.PlatformBellNotificationPublisher;
import projectlx.co.zw.shared_library.utils.requests.PlatformBellNotificationRequest;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.inventory.management.clients.UserManagementServiceClient;
import projectlx.inventory.management.utils.requests.NotificationRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class PlatformBellNotificationSupport {

    private static final String SOURCE_SERVICE = "ldms-inventory-management";
    private static final String NOTIFICATIONS_EXCHANGE = "notifications.direct";
    private static final String NOTIFICATIONS_ROUTING_KEY = "notifications.send";

    private final PlatformBellNotificationPublisher platformBellNotificationPublisher;
    private final UserManagementServiceClient userManagementServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${ldms.portal.base-url:http://localhost:4201}")
    private String portalBaseUrl;

    public PlatformBellNotificationSupport(
            PlatformBellNotificationPublisher platformBellNotificationPublisher,
            UserManagementServiceClient userManagementServiceClient,
            RabbitTemplate rabbitTemplate) {
        this.platformBellNotificationPublisher = platformBellNotificationPublisher;
        this.userManagementServiceClient = userManagementServiceClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void notifyUser(
            Long userId,
            Long organizationId,
            Long excludeUserId,
            String eventKey,
            String title,
            String body,
            String actionRoute,
            String entityType,
            Long entityId,
            Map<String, Object> channelData) {
        if (userId == null || userId <= 0 || (excludeUserId != null && excludeUserId.equals(userId))) {
            return;
        }
        UserDto user = loadUser(userId);
        if (user == null) {
            return;
        }
        publishBell(userId, organizationId, eventKey, title, body, actionRoute, entityType, entityId);
        publishChannels(user, organizationId, eventKey, actionRoute, channelData);
    }

    public void notifyProcurementApprovers(
            Long organizationId,
            Long excludeUserId,
            String eventKey,
            String title,
            String body,
            String actionRoute,
            String entityType,
            Long entityId,
            Map<String, Object> channelData) {
        if (organizationId == null || organizationId <= 0) {
            return;
        }
        List<UserDto> approvers = loadProcurementApprovers(organizationId);
        if (approvers.isEmpty()) {
            log.debug("[OPERATIONAL-NOTIFY] No procurement approvers for orgId={}", organizationId);
            return;
        }
        Set<String> sentEmails = new LinkedHashSet<>();
        Set<String> sentPhones = new LinkedHashSet<>();
        for (UserDto approver : approvers) {
            if (approver == null || approver.getId() == null) {
                continue;
            }
            if (excludeUserId != null && excludeUserId.equals(approver.getId())) {
                continue;
            }
            publishBell(
                    approver.getId(),
                    organizationId,
                    eventKey,
                    title,
                    body,
                    actionRoute,
                    entityType,
                    entityId);
            publishChannelsDeduped(approver, organizationId, eventKey, actionRoute, channelData, sentEmails, sentPhones);
        }
    }

    public void notifyOrganizationUsers(
            Long organizationId,
            Long excludeUserId,
            String eventKey,
            String title,
            String body,
            String actionRoute,
            String entityType,
            Long entityId,
            Map<String, Object> channelData) {
        if (organizationId == null || organizationId <= 0) {
            return;
        }
        List<UserDto> users = loadOrganizationUsers(organizationId);
        Set<Long> notifiedUsers = new LinkedHashSet<>();
        Set<String> sentEmails = new LinkedHashSet<>();
        Set<String> sentPhones = new LinkedHashSet<>();
        for (UserDto user : users) {
            if (user == null || user.getId() == null || !notifiedUsers.add(user.getId())) {
                continue;
            }
            if (excludeUserId != null && excludeUserId.equals(user.getId())) {
                continue;
            }
            publishBell(
                    user.getId(),
                    organizationId,
                    eventKey,
                    title,
                    body,
                    actionRoute,
                    entityType,
                    entityId);
            publishChannelsDeduped(user, organizationId, eventKey, actionRoute, channelData, sentEmails, sentPhones);
        }
    }

    private void publishBell(
            Long userId,
            Long organizationId,
            String eventKey,
            String title,
            String body,
            String actionRoute,
            String entityType,
            Long entityId) {
        PlatformBellNotificationRequest request = new PlatformBellNotificationRequest(
                buildSourceEventId(eventKey, entityType, entityId, userId),
                userId,
                organizationId,
                eventKey,
                title,
                body,
                actionRoute,
                entityType,
                entityId,
                SOURCE_SERVICE);
        platformBellNotificationPublisher.publish(request);
    }

    private void publishChannels(
            UserDto user,
            Long organizationId,
            String templateKey,
            String actionRoute,
            Map<String, Object> channelData) {
        publishChannelsDeduped(user, organizationId, templateKey, actionRoute, channelData, new LinkedHashSet<>(), new LinkedHashSet<>());
    }

    private void publishChannelsDeduped(
            UserDto user,
            Long organizationId,
            String templateKey,
            String actionRoute,
            Map<String, Object> channelData,
            Set<String> sentEmails,
            Set<String> sentPhones) {
        String email = normalizeEmail(user.getEmail());
        String phone = safe(user.getPhoneNumber());
        boolean hasEmail = StringUtils.hasText(email) && sentEmails.add(email.toLowerCase(Locale.ROOT));
        boolean hasPhone = StringUtils.hasText(phone) && sentPhones.add(normalizePhone(phone));

        if (!hasEmail && !hasPhone) {
            log.debug("[OPERATIONAL-NOTIFY] Skipping channels for userId={} template={}: no contact details",
                    user.getId(), templateKey);
            return;
        }

        Map<String, Object> data = enrichTemplateData(channelData, actionRoute, user);
        String recipientUserId = user.getId() != null ? user.getId().toString() : templateKey;
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID().toString(),
                templateKey,
                new NotificationRequest.Recipient(
                        recipientUserId,
                        hasEmail ? email : null,
                        hasPhone ? phone : null,
                        null),
                data,
                new NotificationRequest.Metadata(SOURCE_SERVICE, null));

        try {
            log.info("[OPERATIONAL-NOTIFY] Publishing template={} userId={} email={} phone={}",
                    templateKey, user.getId(), hasEmail ? email : "-", hasPhone ? phone : "-");
            rabbitTemplate.convertAndSend(NOTIFICATIONS_EXCHANGE, NOTIFICATIONS_ROUTING_KEY, request);
        } catch (Exception ex) {
            log.error("[OPERATIONAL-NOTIFY] Failed template={} userId={}: {}", templateKey, user.getId(), ex.getMessage());
        }
    }

    private Map<String, Object> enrichTemplateData(Map<String, Object> channelData, String actionRoute, UserDto user) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (channelData != null) {
            data.putAll(channelData);
        }
        data.putIfAbsent("signInLink", buildSignInLink(actionRoute));
        data.put("contactName", displayName(user));
        data.put("email", safe(user.getEmail()));
        data.put("Email", safe(user.getEmail()));
        data.put("phoneNumber", safe(user.getPhoneNumber()));
        return data;
    }

    private String buildSignInLink(String actionRoute) {
        String base = safe(portalBaseUrl).replaceAll("/+$", "");
        if (!StringUtils.hasText(actionRoute)) {
            return base;
        }
        String route = actionRoute.startsWith("/") ? actionRoute : "/" + actionRoute;
        return base + route;
    }

    private UserDto loadUser(Long userId) {
        try {
            UserResponse response = userManagementServiceClient.findById(userId, Locale.ENGLISH);
            if (response != null && response.isSuccess() && response.getUserDto() != null) {
                return response.getUserDto();
            }
        } catch (Exception ex) {
            log.warn("[OPERATIONAL-NOTIFY] Failed loading userId={}: {}", userId, ex.getMessage());
        }
        return null;
    }

    private List<UserDto> loadProcurementApprovers(Long organizationId) {
        try {
            UserResponse response = userManagementServiceClient.findProcurementApproversByOrganization(
                    organizationId, Locale.ENGLISH);
            if (response != null && response.isSuccess() && response.getUserDtoList() != null) {
                return response.getUserDtoList();
            }
        } catch (Exception ex) {
            log.warn("[OPERATIONAL-NOTIFY] Failed loading procurement approvers for orgId={}: {}",
                    organizationId, ex.getMessage());
        }
        return List.of();
    }

    private List<UserDto> loadOrganizationUsers(Long organizationId) {
        try {
            UserResponse response = userManagementServiceClient.findByOrganizationId(organizationId, Locale.ENGLISH);
            if (response != null && response.isSuccess() && response.getUserDtoList() != null) {
                return response.getUserDtoList();
            }
        } catch (Exception ex) {
            log.warn("[OPERATIONAL-NOTIFY] Failed loading organisation users for orgId={}: {}",
                    organizationId, ex.getMessage());
        }
        return new ArrayList<>();
    }

    private static String buildSourceEventId(String eventKey, String entityType, Long entityId, Long userId) {
        return eventKey + ":" + entityType + ":" + entityId + ":" + userId;
    }

    private static String displayName(UserDto user) {
        String first = safe(user.getFirstName());
        String last = safe(user.getLastName());
        String combined = (first + " " + last).trim();
        if (StringUtils.hasText(combined)) {
            return combined;
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "there";
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim();
    }

    private static String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return phone.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
