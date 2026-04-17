package projectlx.co.zw.audittrail.business.logic.api;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import projectlx.co.zw.audittrail.model.AuditLog;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogFilter;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogServiceStats;

public interface AuditLogQueryService {

    Page<AuditLog> search(AuditLogFilter filter);

    Optional<AuditLog> findById(Long id);

    List<AuditLog> findByTraceIdOrdered(String traceId);

    AuditLogServiceStats buildServiceStats(String serviceName, int hours);
}
