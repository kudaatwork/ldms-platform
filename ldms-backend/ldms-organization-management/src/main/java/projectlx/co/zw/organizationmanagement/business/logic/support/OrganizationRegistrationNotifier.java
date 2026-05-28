package projectlx.co.zw.organizationmanagement.business.logic.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.utils.config.OrganizationPortalLinkProperties;
import projectlx.co.zw.organizationmanagement.utils.requests.NotificationRequest;

/**
 * Sends organisation registration confirmation emails via the notifications service.
 */
@Component
@RequiredArgsConstructor
public class OrganizationRegistrationNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrganizationRegistrationNotifier.class);

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";
    private static final String TEMPLATE_ADMIN = "ORGANIZATION_REGISTERED_BY_ADMIN";
    private static final String TEMPLATE_SIGNUP = "ORGANIZATION_SIGNUP_RECEIVED";
    private static final String ROLE_ORGANIZATION = "organization";
    private static final String ROLE_CONTACT = "contact-person";

    private final RabbitTemplate rabbitTemplate;
    private final OrganizationPortalLinkProperties portalLinks;

    public void sendRegistrationEmails(Organization org, boolean viaSignup) {
        if (org == null) {
            return;
        }
        String templateKey = viaSignup ? TEMPLATE_SIGNUP : TEMPLATE_ADMIN;

        String organizationEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getEmail());
        if (StringUtils.hasText(organizationEmail)) {
            sendToEmail(org.getId(), organizationEmail, templateKey,
                    buildTemplateData(org, viaSignup, ROLE_ORGANIZATION), ROLE_ORGANIZATION);
        } else {
            log.warn("Skipping organisation registration email for orgId={}: organisation email is blank", org.getId());
        }

        String contactEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getContactPersonEmail());
        if (!StringUtils.hasText(contactEmail)) {
            log.warn("Skipping contact person registration email for orgId={}: contact person email is blank", org.getId());
            return;
        }
        if (OrganizationNotificationEmailSupport.isSameEmail(organizationEmail, contactEmail)) {
            log.info(
                    "Organisation and contact person share email {}; sending contact-person registration copy as well",
                    organizationEmail);
        }
        sendToEmail(org.getId(), contactEmail, templateKey,
                buildTemplateData(org, viaSignup, ROLE_CONTACT), ROLE_CONTACT);
    }

    private Map<String, Object> buildTemplateData(Organization org, boolean viaSignup, String recipientRole) {
        Map<String, Object> data = new LinkedHashMap<>();
        String contactName = String.join(" ",
                        safe(org.getContactPersonFirstName()),
                        safe(org.getContactPersonLastName()))
                .trim();
        if (!StringUtils.hasText(contactName)) {
            contactName = safe(org.getName());
        }
        data.put("organizationName", safe(org.getName()));
        data.put("contactName", contactName);
        data.put("organizationEmail", safe(org.getEmail()));
        data.put("contactPersonEmail", safe(org.getContactPersonEmail()));
        data.put("viaSignup", viaSignup);
        data.put("recipientRole", recipientRole);
        data.put("isContactPersonRecipient", ROLE_CONTACT.equals(recipientRole));
        if (ROLE_CONTACT.equals(recipientRole)) {
            data.put("greetingName", contactName);
            data.put("emailPurpose", viaSignup
                    ? "You are registered as the primary contact for this organisation on the LX platform."
                    : "You are registered as the primary contact for this organisation.");
        } else {
            data.put("greetingName", safe(org.getName()));
            data.put("emailPurpose", viaSignup
                    ? "Your organisation signup request was received on the LX platform."
                    : "Your organisation was registered on the LX platform.");
        }
        if (viaSignup) {
            data.put("nextStepsLink", portalLinks.platformSignupUrl());
            data.put("signInLink", portalLinks.platformSignInUrl());
        } else {
            data.put("nextStepsLink", portalLinks.adminSignInUrl());
            data.put("signInLink", portalLinks.adminSignInUrl());
        }
        return data;
    }

    private void sendToEmail(
            Long organizationId,
            String email,
            String templateKey,
            Map<String, Object> baseData,
            String recipientRole) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String normalizedEmail = OrganizationNotificationEmailSupport.normalizeEmail(email);
        Map<String, Object> data = new LinkedHashMap<>(baseData);
        data.put("email", normalizedEmail);
        data.put("Email", normalizedEmail);
        data.put("recipientRole", recipientRole);
        String recipientUserId = organizationId != null
                ? organizationId + ":" + recipientRole
                : recipientRole;
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                recipientUserId,
                normalizedEmail,
                null,
                null);
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID().toString(),
                templateKey,
                recipient,
                data,
                new NotificationRequest.Metadata("ldms-organization-management", null));
        try {
            log.info("Publishing organisation registration email template={} role={} to={}",
                    templateKey, recipientRole, normalizedEmail);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        } catch (Exception e) {
            log.error("Failed to publish organisation registration email template={} role={} to={}: {}",
                    templateKey, recipientRole, normalizedEmail, e.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
