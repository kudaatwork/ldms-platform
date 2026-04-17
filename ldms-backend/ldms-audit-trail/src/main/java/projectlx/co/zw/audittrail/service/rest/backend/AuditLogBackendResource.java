package projectlx.co.zw.audittrail.service.rest.backend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.audittrail.service.processor.api.AuditLogProcessor;
import projectlx.co.zw.audittrail.utils.requests.AuditLogSearchRequest;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/backend/audit-log")
@Tag(name = "Audit Log Backend Resource", description = "Inter-service (backend) operations for querying audit logs")
@RequiredArgsConstructor
public class AuditLogBackendResource {

    private static final String BACKEND_USER = "BACKEND";

    private final AuditLogProcessor auditLogProcessor;

    @Auditable(action = "SEARCH_AUDIT_LOGS_BACKEND")
    @GetMapping("/search")
    @Operation(summary = "Search audit logs (backend)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters")
    })
    public AuditLogResponse search(
            @ModelAttribute AuditLogSearchRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.search(request, locale, BACKEND_USER);
    }

    @Auditable(action = "FIND_AUDIT_LOG_BY_ID_BACKEND")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find audit log by id (backend)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request completed"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public AuditLogResponse findById(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.findById(id, locale, BACKEND_USER);
    }

    @Auditable(action = "FIND_AUDIT_LOGS_BY_TRACE_BACKEND")
    @GetMapping("/trace/{traceId}")
    @Operation(summary = "Find audit logs by trace id (backend)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request completed"),
            @ApiResponse(responseCode = "400", description = "Invalid trace id")
    })
    public AuditLogResponse findByTrace(
            @PathVariable String traceId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.findByTraceId(traceId, locale, BACKEND_USER);
    }

    @Auditable(action = "AUDIT_LOG_SERVICE_STATS_BACKEND")
    @GetMapping("/service/{serviceName}/stats")
    @Operation(summary = "Service audit statistics (backend)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request completed"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public AuditLogResponse getServiceStats(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "24") int hours,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.getServiceStats(serviceName, hours, locale, BACKEND_USER);
    }
}
