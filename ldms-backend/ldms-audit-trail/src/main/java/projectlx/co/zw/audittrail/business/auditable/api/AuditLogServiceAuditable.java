package projectlx.co.zw.audittrail.business.auditable.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import projectlx.co.zw.audittrail.model.AuditEventType;
import projectlx.co.zw.audittrail.model.AuditLog;

public interface AuditLogServiceAuditable {
    AuditLog create(AuditLog auditLog, Locale locale, String username);

    AuditLog update(AuditLog auditLog, Locale locale, String username);

    AuditLog delete(AuditLog auditLog, Locale locale, String username);

    Page<AuditLog> search(
            String serviceName,
            String username,
            AuditEventType eventType,
            Integer httpStatusCode,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    Optional<AuditLog> findById(Long id);

    List<AuditLog> findByTraceIdOrdered(String traceId);

    long countInWindow(String serviceName, LocalDateTime from);

    long countHttpErrorsInWindow(String serviceName, LocalDateTime from);

    Double avgResponseTimeMsInWindow(String serviceName, LocalDateTime from);

    List<Object[]> countByEventTypeInWindow(String serviceName, LocalDateTime from);

    List<Object[]> countByHttpStatusInWindow(String serviceName, LocalDateTime from);
}
