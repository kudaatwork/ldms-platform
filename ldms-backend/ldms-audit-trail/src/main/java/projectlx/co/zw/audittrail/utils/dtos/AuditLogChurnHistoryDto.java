package projectlx.co.zw.audittrail.utils.dtos;

import java.time.LocalDateTime;

public record AuditLogChurnHistoryDto(
        Long id,
        String batchReference,
        String triggerType,
        String triggeredBy,
        LocalDateTime triggeredAt,
        Long deletedLogCount,
        LocalDateTime oldestRequestTimestamp,
        LocalDateTime newestRequestTimestamp,
        String churnStatus,
        Long jobExecutionId,
        String failureReason,
        LocalDateTime completedAt) {}
