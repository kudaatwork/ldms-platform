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
 * Sends organisation KYC stage emails to the organisation email and contact person email.
 */
@Component
@RequiredArgsConstructor
public class OrganizationKycNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrganizationKycNotifier.class);

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";
    private static final String TEMPLATE_STAGE1_APPROVED = "ORG_KYC_STAGE1_APPROVED";
    private static final String TEMPLATE_STAGE2_APPROVED = "ORG_KYC_STAGE2_APPROVED";

    private final RabbitTemplate rabbitTemplate;
    private final OrganizationPortalLinkProperties portalLinks;

    public void sendStage1Approved(Organization org) {
        sendKycEmails(org, TEMPLATE_STAGE1_APPROVED, true);
    }

    public void sendStage2Approved(Organization org) {
        sendKycEmails(org, TEMPLATE_STAGE2_APPROVED, true);
    }

    private void sendKycEmails(Organization org, String templateKey, boolean viaSignup) {
        if (org == null) {
            return;
        }
        Map<String, Object> baseData = buildTemplateData(org, viaSignup);
        String organizationEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getEmail());
        if (StringUtils.hasText(organizationEmail)) {
            sendToEmail(org.getId(), organizationEmail, templateKey, baseData, "organization");
        }
        String contactEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getContactPersonEmail());
        if (StringUtils.hasText(contactEmail)
                && !OrganizationNotificationEmailSupport.isSameEmail(organizationEmail, contactEmail)) {
            sendToEmail(org.getId(), contactEmail, templateKey, baseData, "contact-person");
        }
    }

    private Map<String, Object> buildTemplateData(Organization org, boolean viaSignup) {
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
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID().toString(),
                templateKey,
                new NotificationRequest.Recipient(
                        recipientUserId,
                        normalizedEmail,
                        null,
                        null),
                data,
                new NotificationRequest.Metadata("ldms-organization-management", null));
        try {
            log.info("Publishing organisation KYC email template={} role={} to={}",
                    templateKey, recipientRole, normalizedEmail);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        } catch (Exception e) {
            log.error("Failed to publish organisation KYC email template={} role={} to={}: {}",
                    templateKey, recipientRole, normalizedEmail, e.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
