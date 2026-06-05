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
 * Sends organisation KYC lifecycle emails (submission, per-stage approval, rejection, resubmission).
 */
@Component
@RequiredArgsConstructor
public class OrganizationKycNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrganizationKycNotifier.class);

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";
    private static final String TEMPLATE_SUBMITTED = "ORG_KYC_SUBMITTED";
    private static final String TEMPLATE_STAGE1_APPROVED = "ORG_KYC_STAGE1_APPROVED";
    private static final String TEMPLATE_STAGE_APPROVED = "ORG_KYC_STAGE_APPROVED";
    private static final String TEMPLATE_FULLY_APPROVED = "ORG_KYC_STAGE2_APPROVED";
    private static final String TEMPLATE_APPROVED_CREDENTIALS = "ORG_KYC_APPROVED_CREDENTIALS";
    private static final String TEMPLATE_ORG_EMAIL_VERIFICATION = "ORG_EMAIL_VERIFICATION";
    private static final String TEMPLATE_REJECTED = "ORG_KYC_REJECTED";
    private static final String TEMPLATE_RESUBMISSION_ALLOWED = "ORG_KYC_RESUBMISSION_ALLOWED";

    private final RabbitTemplate rabbitTemplate;
    private final OrganizationPortalLinkProperties portalLinks;

    public void sendKycSubmitted(Organization org) {
        if (org == null) {
            return;
        }
        Map<String, Object> data = buildTemplateData(org);
        data.put("statusHeadline", "Application submitted");
        data.put(
                "statusDetail",
                "Thank you — your KYC application for "
                        + safe(org.getName())
                        + " is in our review queue. Track live progress on your onboarding page; we will email you after each approval stage.");
        sendKycEmailsWithData(org, TEMPLATE_SUBMITTED, data);
    }

    public void sendStageApproved(Organization org, int approvedStage, int totalStages) {
        if (org == null || approvedStage < 1 || totalStages < 1) {
            return;
        }
        if (approvedStage >= totalStages) {
            sendFullyApproved(org);
            return;
        }
        Map<String, Object> data = buildTemplateData(org);
        enrichStageApprovalData(data, approvedStage, totalStages, false);
        String template = approvedStage == 1 ? TEMPLATE_STAGE1_APPROVED : TEMPLATE_STAGE_APPROVED;
        sendKycEmailsWithData(org, template, data);
    }

    public void sendFullyApproved(Organization org) {
        if (org == null) {
            return;
        }
        Map<String, Object> data = buildTemplateData(org);
        data.put("statusHeadline", "Organisation verified");
        data.put(
                "statusDetail",
                "Your organisation is fully approved on Project LX. Use the separate email with your temporary sign-in details to access the platform.");
        sendKycEmailsWithData(org, TEMPLATE_FULLY_APPROVED, data);
    }

    public void sendApprovedCredentials(Organization org, String temporaryUsername, String temporaryPassword) {
        if (org == null || !StringUtils.hasText(temporaryUsername) || !StringUtils.hasText(temporaryPassword)) {
            return;
        }
        Map<String, Object> data = buildCredentialsTemplateData(org, temporaryUsername, temporaryPassword);
        sendKycEmailsWithData(org, TEMPLATE_APPROVED_CREDENTIALS, data);
    }

    /** Sends temporary portal credentials to the contact person only (supplier-registered onboarding). */
    public void sendContactCredentials(Organization org, String temporaryUsername, String temporaryPassword) {
        if (org == null || !StringUtils.hasText(temporaryUsername) || !StringUtils.hasText(temporaryPassword)) {
            return;
        }
        String contactEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getContactPersonEmail());
        if (!StringUtils.hasText(contactEmail)) {
            log.warn("Skipping contact credentials email for orgId={}: contact email is blank", org.getId());
            return;
        }
        Map<String, Object> data = buildCredentialsTemplateData(org, temporaryUsername, temporaryPassword);
        sendToEmail(org.getId(), contactEmail, TEMPLATE_APPROVED_CREDENTIALS, data, "contact-person");
    }

    /** Sends organisation email verification link to the organisation inbox only. */
    public void sendOrganizationEmailVerification(Organization org, String verificationLink) {
        if (org == null || !StringUtils.hasText(verificationLink)) {
            return;
        }
        String organizationEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getEmail());
        if (!StringUtils.hasText(organizationEmail)) {
            log.warn("Skipping organisation verification email for orgId={}: organisation email is blank", org.getId());
            return;
        }
        Map<String, Object> data = buildTemplateData(org);
        data.put("statusHeadline", "Verify your organisation email");
        data.put(
                "statusDetail",
                "Please confirm that "
                        + safe(org.getName())
                        + " is registered on Project LX LDMS. Open the link below to verify your organisation email.");
        data.put("verificationLink", verificationLink.trim());
        sendToEmail(org.getId(), organizationEmail, TEMPLATE_ORG_EMAIL_VERIFICATION, data, "organization");
    }

    private Map<String, Object> buildCredentialsTemplateData(
            Organization org, String temporaryUsername, String temporaryPassword) {
        Map<String, Object> data = buildTemplateData(org);
        data.put("statusHeadline", "Your portal access is ready");
        data.put(
                "statusDetail",
                "Your organisation "
                        + safe(org.getName())
                        + " is registered on Project LX. Sign in with the temporary username and password below, then choose your permanent credentials.");
        data.put("temporaryUsername", temporaryUsername.trim());
        data.put("temporaryPassword", temporaryPassword.trim());
        return data;
    }

    public void sendRejected(Organization org, String rejectionReason) {
        if (org == null || !StringUtils.hasText(rejectionReason)) {
            return;
        }
        Map<String, Object> data = buildTemplateData(org);
        data.put("rejectionReason", rejectionReason.trim());
        data.put("statusHeadline", "Application not approved");
        sendKycEmailsWithData(org, TEMPLATE_REJECTED, data);
    }

    public void sendResubmissionAllowed(Organization org, String resubmissionNotes) {
        if (org == null) {
            return;
        }
        Map<String, Object> data = buildTemplateData(org);
        String notes = StringUtils.hasText(resubmissionNotes)
                ? resubmissionNotes.trim()
                : "You may update your organisation profile and submit KYC again.";
        data.put("resubmissionNotes", notes);
        sendKycEmailsWithData(org, TEMPLATE_RESUBMISSION_ALLOWED, data);
    }

    private void enrichStageApprovalData(Map<String, Object> data, int approvedStage, int totalStages, boolean isFinal) {
        int nextStage = Math.min(approvedStage + 1, totalStages);
        data.put("approvedStageNumber", approvedStage);
        data.put("totalStages", totalStages);
        data.put("nextStageNumber", isFinal ? "" : nextStage);
        data.put("stageLabel", "Stage " + approvedStage);
        data.put("nextStageLabel", isFinal ? "Complete" : "Stage " + nextStage + " review");
        data.put("statusHeadline", "Stage " + approvedStage + " approved");
        if (isFinal) {
            data.put("statusDetail", "Your organisation has completed all required approval stages.");
        } else {
            data.put(
                    "statusDetail",
                    "Stage " + approvedStage + " of " + totalStages + " is complete. Your application is now in "
                            + data.get("nextStageLabel") + ".");
        }
        data.put("isFinalApproval", isFinal);
    }

    private void sendKycEmailsWithData(Organization org, String templateKey, Map<String, Object> baseData) {
        if (org == null || baseData == null) {
            return;
        }
        String organizationEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getEmail());
        String contactEmail = OrganizationNotificationEmailSupport.normalizeEmail(org.getContactPersonEmail());
        boolean sentOrganization = false;
        if (StringUtils.hasText(organizationEmail)) {
            sendToEmail(org.getId(), organizationEmail, templateKey, baseData, "organization");
            sentOrganization = true;
        } else {
            log.warn("Skipping organisation KYC email template={} for orgId={}: organisation email is blank",
                    templateKey, org.getId());
        }
        if (!StringUtils.hasText(contactEmail)) {
            log.warn("Skipping contact person KYC email template={} for orgId={}: contact email is blank",
                    templateKey, org.getId());
            return;
        }
        if (OrganizationNotificationEmailSupport.isSameEmail(organizationEmail, contactEmail)) {
            if (sentOrganization) {
                log.info(
                        "Organisation and contact person share email {}; KYC notification already sent once",
                        organizationEmail);
            }
            return;
        }
        sendToEmail(org.getId(), contactEmail, templateKey, baseData, "contact-person");
    }

    private Map<String, Object> buildTemplateData(Organization org) {
        Map<String, Object> data = new LinkedHashMap<>();
        String contactName = String.join(
                        " ",
                        safe(org.getContactPersonFirstName()),
                        safe(org.getContactPersonLastName()))
                .trim();
        if (!StringUtils.hasText(contactName)) {
            contactName = safe(org.getName());
        }
        data.put("organizationName", safe(org.getName()));
        data.put("contactName", contactName);
        data.put("nextStepsLink", portalLinks.nextStepsUrlFor(org));
        data.put("signInLink", portalLinks.signInUrlFor(org));
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
