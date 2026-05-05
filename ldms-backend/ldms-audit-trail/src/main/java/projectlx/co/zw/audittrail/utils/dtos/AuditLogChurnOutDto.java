package projectlx.co.zw.audittrail.utils.dtos;

import java.time.LocalDateTime;

public record AuditLogChurnOutDto(
        Long historyId,
        String triggerType,
        String triggeredBy,
        LocalDateTime triggeredAt,
        Long deletedLogCount,
        LocalDateTime oldestRequestTimestamp,
        LocalDateTime newestRequestTimestamp,
        String batchReference) {}
