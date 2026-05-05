package projectlx.co.zw.audittrail.repository;

import java.time.LocalDateTime;

public interface AuditLogRangeProjection {
    LocalDateTime getOldestRequestTimestamp();

    LocalDateTime getNewestRequestTimestamp();
}
