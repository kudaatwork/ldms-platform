package projectlx.co.zw.audittrail.service.tasks;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import projectlx.co.zw.audittrail.service.processor.api.AuditLogProcessor;
import projectlx.co.zw.audittrail.utils.config.LdmsAuditProperties;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;

@Component
@RequiredArgsConstructor
public class AuditLogChurnScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogChurnScheduler.class);

    private final AuditLogProcessor auditLogProcessor;
    private final LdmsAuditProperties ldmsAuditProperties;

    @Scheduled(
            fixedDelayString = "${ldms.audit.churn.fixed-delay-ms:2592000000}",
            initialDelayString = "${ldms.audit.churn.initial-delay-ms:2592000000}")
    public void churnOutRequestLogsScheduled() {
        if (!ldmsAuditProperties.getChurn().isSchedulerEnabled()) {
            return;
        }
        try {
            AuditLogResponse response = auditLogProcessor.churnOutRequestLogs(Locale.ENGLISH, "SYSTEM", "SCHEDULED");
            logger.info(
                    "Scheduled churn job launched. statusCode={}, success={}, jobExecutionId={}, batchReference={}",
                    response.getStatusCode(),
                    response.isSuccess(),
                    response.getChurnLaunch() != null ? response.getChurnLaunch().jobExecutionId() : null,
                    response.getChurnLaunch() != null ? response.getChurnLaunch().batchReference() : null);
        } catch (Exception ex) {
            logger.error("Scheduled churn out failed: {}", ex.getMessage(), ex);
        }
    }
}
