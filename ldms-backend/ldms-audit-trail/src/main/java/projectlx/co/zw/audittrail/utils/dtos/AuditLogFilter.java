package projectlx.co.zw.audittrail.utils.dtos;

import java.time.LocalDateTime;

public record AuditLogFilter(
        String serviceName,
        String username,
        String eventType,
        Integer httpStatusCode,
        LocalDateTime from,
        LocalDateTime to,
        String searchValue,
        /** Substring match (case-insensitive); null = no filter. */
        String action,
        String requestUrl,
        String httpMethod,
        String traceId,
        int page,
        int size,
        String sortBy,
        String sortDir) {}
