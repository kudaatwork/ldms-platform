package projectlx.co.zw.audittrail.service.batch;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import projectlx.co.zw.audittrail.model.AuditLogChurnHistory;
import projectlx.co.zw.audittrail.repository.AuditLogChurnHistoryRepository;
import projectlx.co.zw.audittrail.repository.AuditLogRangeProjection;
import projectlx.co.zw.audittrail.repository.AuditLogRepository;
import projectlx.co.zw.audittrail.utils.enums.AuditLogChurnStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Component
public class AuditLogChurnSnapshotTasklet implements Tasklet {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogChurnHistoryRepository churnHistoryRepository;

    public AuditLogChurnSnapshotTasklet(
            AuditLogRepository auditLogRepository, AuditLogChurnHistoryRepository churnHistoryRepository) {
        this.auditLogRepository = auditLogRepository;
        this.churnHistoryRepository = churnHistoryRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var jobExecution = contribution.getStepExecution().getJobExecution();
        var params = jobExecution.getJobParameters();

        String triggerType = params.getString("triggerType");
        String triggeredBy = params.getString("triggeredBy");
        String batchReference = params.getString("batchReference");
        LocalDateTime purgeBefore = parsePurgeBefore(params.getString("purgeBefore"));

        long total = purgeBefore == null ? auditLogRepository.count() : auditLogRepository.countBefore(purgeBefore);
        AuditLogRangeProjection range =
                purgeBefore == null ? auditLogRepository.findTimestampRange() : auditLogRepository.findTimestampRangeBefore(purgeBefore);
        LocalDateTime now = LocalDateTime.now();

        AuditLogChurnHistory history = churnHistoryRepository.save(AuditLogChurnHistory.builder()
                .batchReference(batchReference != null ? batchReference : "unknown")
                .triggerType(triggerType != null ? triggerType : "SYSTEM")
                .triggeredBy(triggeredBy != null ? triggeredBy : "SYSTEM")
                .triggeredAt(now)
                .deletedLogCount(0L)
                .oldestRequestTimestamp(range != null ? range.getOldestRequestTimestamp() : null)
                .newestRequestTimestamp(range != null ? range.getNewestRequestTimestamp() : null)
                .churnStatus(AuditLogChurnStatus.RUNNING)
                .jobExecutionId(jobExecution.getId())
                .failureReason(null)
                .completedAt(null)
                .entityStatus(EntityStatus.ACTIVE)
                .createdAt(now)
                .createdBy(triggeredBy != null ? triggeredBy : "SYSTEM")
                .modifiedAt(now)
                .modifiedBy(triggeredBy != null ? triggeredBy : "SYSTEM")
                .build());

        jobExecution.getExecutionContext().putLong(AuditLogChurnJobKeys.CHURN_HISTORY_ID, history.getId());
        jobExecution.getExecutionContext().putLong(AuditLogChurnJobKeys.SNAPSHOT_TOTAL_ROWS, total);

        return RepeatStatus.FINISHED;
    }

    private static LocalDateTime parsePurgeBefore(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
