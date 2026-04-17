package projectlx.co.zw.audittrail.utils.dtos;

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
