package projectlx.co.zw.audittrail.service.batch;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;
import projectlx.co.zw.audittrail.model.AuditLogChurnHistory;
import projectlx.co.zw.audittrail.repository.AuditLogChurnHistoryRepository;
import projectlx.co.zw.audittrail.utils.enums.AuditLogChurnStatus;

@Component
@RequiredArgsConstructor
public class AuditLogChurnJobExecutionListener implements JobExecutionListener {

    private static final int MAX_FAILURE_REASON = 2400;

    private final AuditLogChurnHistoryRepository churnHistoryRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // no-op
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.FAILED && jobExecution.getStatus() != BatchStatus.STOPPED) {
            return;
        }
        var executionContext = jobExecution.getExecutionContext();
        if (!executionContext.containsKey(AuditLogChurnJobKeys.CHURN_HISTORY_ID)) {
            return;
        }
        Long historyId = executionContext.getLong(AuditLogChurnJobKeys.CHURN_HISTORY_ID);
        if (historyId == null || historyId == 0) {
            return;
        }
        churnHistoryRepository.findById(historyId).ifPresent(history -> {
            if (history.getChurnStatus() != AuditLogChurnStatus.RUNNING) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            history.setChurnStatus(AuditLogChurnStatus.FAILED);
            history.setCompletedAt(now);
            history.setModifiedAt(now);
            history.setModifiedBy(history.getTriggeredBy());
            String exit = jobExecution.getExitStatus().getExitDescription();
            history.setFailureReason(truncate(exit != null ? exit : jobExecution.getStatus().name()));
            churnHistoryRepository.save(history);
        });
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= MAX_FAILURE_REASON ? s : s.substring(0, MAX_FAILURE_REASON);
    }
}
