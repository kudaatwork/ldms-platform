package projectlx.co.zw.audittrail.service.tasks;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import projectlx.co.zw.audittrail.business.logic.impl.AuditLogChurnLaunchService;
import projectlx.co.zw.audittrail.utils.config.LdmsAuditProperties;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;

@Component
@RequiredArgsConstructor
public class AuditLogChurnScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogChurnScheduler.class);

    private final AuditLogChurnLaunchService auditLogChurnLaunchService;
    private final LdmsAuditProperties ldmsAuditProperties;

    @Scheduled(
            fixedDelayString = "${ldms.audit.churn.fixed-delay-ms:7776000000}",
            initialDelayString = "${ldms.audit.churn.initial-delay-ms:7776000000}")
    public void churnOutRequestLogsScheduled() {
        if (!ldmsAuditProperties.getChurn().isSchedulerEnabled()) {
            return;
        }
        try {
            int retentionDays = ldmsAuditProperties.getChurn().getRetentionDays();
            AuditLogResponse response = auditLogChurnLaunchService.launchRetention(
                    Locale.ENGLISH, "SYSTEM", "SCHEDULED", retentionDays);
            logger.info(
                    "Scheduled retention churn launched. retentionDays={}, statusCode={}, success={}, jobExecutionId={}, batchReference={}",
                    retentionDays,
                    response.getStatusCode(),
                    response.isSuccess(),
                    response.getChurnLaunch() != null ? response.getChurnLaunch().jobExecutionId() : null,
                    response.getChurnLaunch() != null ? response.getChurnLaunch().batchReference() : null);
        } catch (Exception ex) {
            logger.error("Scheduled churn out failed: {}", ex.getMessage(), ex);
        }
    }
}
