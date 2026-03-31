package projectlx.co.zw.audittrail.dto;

import java.time.LocalDateTime;

public record AuditLogFilter(
        String serviceName,
        String username,
        String eventType,
        Integer httpStatusCode,
        LocalDateTime from,
        LocalDateTime to,
        int page,
        int size,
        String sortBy,
        String sortDir) {}
