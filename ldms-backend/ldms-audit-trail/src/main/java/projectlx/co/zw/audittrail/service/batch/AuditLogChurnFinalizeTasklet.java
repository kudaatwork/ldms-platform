package projectlx.co.zw.audittrail.service.batch;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import projectlx.co.zw.audittrail.model.AuditLogChurnHistory;
import projectlx.co.zw.audittrail.repository.AuditLogChurnHistoryRepository;
import projectlx.co.zw.audittrail.utils.enums.AuditLogChurnStatus;

@Component
@RequiredArgsConstructor
public class AuditLogChurnFinalizeTasklet implements Tasklet {

    public static final String DELETE_STEP_NAME = "deleteAuditLogsStep";

    private final AuditLogChurnHistoryRepository churnHistoryRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var jobExecution = contribution.getStepExecution().getJobExecution();
        var executionContext = jobExecution.getExecutionContext();
        if (!executionContext.containsKey(AuditLogChurnJobKeys.CHURN_HISTORY_ID)) {
            return RepeatStatus.FINISHED;
        }
        Long historyId = executionContext.getLong(AuditLogChurnJobKeys.CHURN_HISTORY_ID);

        long deleted = jobExecution.getStepExecutions().stream()
                .filter(se -> DELETE_STEP_NAME.equals(se.getStepName()))
                .mapToLong(StepExecution::getWriteCount)
                .findFirst()
                .orElse(0L);

        if (historyId == null || historyId == 0) {
            return RepeatStatus.FINISHED;
        }

        AuditLogChurnHistory history =
                churnHistoryRepository.findById(historyId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        history.setDeletedLogCount(deleted);
        history.setChurnStatus(AuditLogChurnStatus.SUCCESS);
        history.setCompletedAt(now);
        history.setModifiedAt(now);
        history.setModifiedBy(history.getTriggeredBy());
        churnHistoryRepository.save(history);

        return RepeatStatus.FINISHED;
    }
}
