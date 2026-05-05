package projectlx.co.zw.audittrail.utils.dtos;

import java.time.LocalDateTime;

/** Returned immediately when a churn Spring Batch job is accepted (async execution). */
public record AuditLogChurnLaunchDto(
        Long jobExecutionId,
        String batchReference,
        LocalDateTime acceptedAt,
        String triggerType,
        String triggeredBy,
        String message) {}
