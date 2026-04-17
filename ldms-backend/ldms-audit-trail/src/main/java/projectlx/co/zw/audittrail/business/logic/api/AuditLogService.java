package projectlx.co.zw.audittrail.business.logic.api;

import java.util.Locale;
import projectlx.co.zw.audittrail.utils.requests.AuditLogSearchRequest;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;

public interface AuditLogService {

    AuditLogResponse search(AuditLogSearchRequest request, Locale locale, String username);

    AuditLogResponse findById(Long id, Locale locale, String username);

    AuditLogResponse findByTraceId(String traceId, Locale locale, String username);

    AuditLogResponse getServiceStats(String serviceName, int hours, Locale locale, String username);
}
