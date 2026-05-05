package projectlx.co.zw.audittrail.business.validation.api;

import java.util.Locale;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogFilter;
import projectlx.co.zw.audittrail.utils.requests.AuditLogMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

public interface AuditLogQueryValidator {

    ValidatorDto validateMultipleFiltersRequest(AuditLogMultipleFiltersRequest request, Locale locale);

    ValidatorDto validateSearch(AuditLogFilter filter, Locale locale);

    ValidatorDto validateId(Long id, Locale locale);

    ValidatorDto validateTraceId(String traceId, Locale locale);

    ValidatorDto validateServiceStats(String serviceName, int hours, Locale locale);
}
