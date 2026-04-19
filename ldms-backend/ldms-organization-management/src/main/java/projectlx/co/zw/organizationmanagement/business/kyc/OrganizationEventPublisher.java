package projectlx.co.zw.organizationmanagement.business.kyc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import projectlx.co.zw.organizationmanagement.model.KycReviewStage;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.OrganizationClassification;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes organization domain events to {@link Constants#EXCHANGE_ORG} (topic).
 * <p>
 * <b>Delivery guarantee:</b> Callers must schedule these methods <em>after</em> the database
 * transaction commits (e.g. {@link org.springframework.transaction.support.TransactionSynchronizationManager}
 * {@code registerSynchronization} with {@code afterCommit}), so a failed broker does not roll back
 * persisted state. Implementations swallow RabbitMQ failures and log only — they never throw.
 */
public class OrganizationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrganizationEventPublisher.class);

    public static final String RK_SUBMITTED = "org.submitted";
    public static final String RK_STAGE1_APPROVED = "org.kyc_stage1_approved";
    public static final String RK_STAGE2_APPROVED = "org.kyc_stage2_approved";
    public static final String RK_VERIFIED = "org.verified";
    public static final String RK_REJECTED = "org.rejected";
    public static final String RK_RESUBMITTED = "org.resubmitted";

    private final RabbitTemplate rabbitTemplate;

    public OrganizationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishSubmitted(Organization org) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("organizationId", org.getId());
        payload.put("classification", nameOrNull(org.getOrganizationClassification()));
        payload.put("submittedAt", org.getSubmittedAt());
        payload.put("resubmissionCycle", org.getCurrentResubmissionCycle());
        payload.put("email", org.getEmail());
        payload.put("contactPersonEmail", org.getContactPersonEmail());
        send(RK_SUBMITTED, payload);
    }

    public void publishStage1Approved(Organization org, String reviewerUsername, LocalDateTime reviewedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("organizationId", org.getId());
        payload.put("reviewerUsername", reviewerUsername);
        payload.put("reviewedAt", reviewedAt);
        payload.put("resubmissionCycle", org.getCurrentResubmissionCycle());
        send(RK_STAGE1_APPROVED, payload);
    }

    public void publishStage2Approved(Organization org, String reviewerUsername, LocalDateTime reviewedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("organizationId", org.getId());
        payload.put("reviewerUsername", reviewerUsername);
        payload.put("reviewedAt", reviewedAt);
        payload.put("resubmissionCycle", org.getCurrentResubmissionCycle());
        send(RK_STAGE2_APPROVED, payload);
    }

    public void publishVerified(Organization org, LocalDateTime verifiedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("organizationId", org.getId());
        payload.put("classification", nameOrNull(org.getOrganizationClassification()));
        payload.put("verifiedAt", verifiedAt);
        send(RK_VERIFIED, payload);
    }

    public void publishRejected(
            Organization org,
            KycReviewStage stage,
            String rejectionReason,
            String reviewerUsername,
            LocalDateTime reviewedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("organizationId", org.getId());
        payload.put("stage", stage != null ? stage.name() : null);
        payload.put("rejectionReason", rejectionReason);
        payload.put("reviewerUsername", reviewerUsername);
        payload.put("reviewedAt", reviewedAt);
        payload.put("resubmissionCycle", org.getCurrentResubmissionCycle());
        send(RK_REJECTED, payload);
    }

    public void publishResubmitted(Organization org, String allowedBy, LocalDateTime allowedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("organizationId", org.getId());
        payload.put("resubmissionCycle", org.getCurrentResubmissionCycle());
        payload.put("allowedBy", allowedBy);
        payload.put("allowedAt", allowedAt);
        send(RK_RESUBMITTED, payload);
    }

    private void send(String routingKey, Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(Constants.EXCHANGE_ORG, routingKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish org event routingKey={} payload={}", routingKey, payload, e);
        }
    }

    private static String nameOrNull(OrganizationClassification c) {
        return c == null ? null : c.name();
    }
}
