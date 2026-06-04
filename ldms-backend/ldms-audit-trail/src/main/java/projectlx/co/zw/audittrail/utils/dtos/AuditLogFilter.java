package projectlx.co.zw.audittrail.utils.dtos;

import java.time.LocalDateTime;
import java.util.List;

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
        String clientPlatform,
        List<String> actionsIn,
        List<String> excludeActions,
        List<String> usernamesIn,
        int page,
        int size,
        String sortBy,
        String sortDir) {}
