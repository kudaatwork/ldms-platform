package projectlx.co.zw.audittrail.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.audittrail.service.processor.api.AuditLogProcessor;
import projectlx.co.zw.audittrail.service.rest.AuditLogChurnHistoryExportResponseFactory;
import projectlx.co.zw.audittrail.service.rest.AuditLogExportResponseFactory;
import projectlx.co.zw.audittrail.utils.requests.AuditLogChurnHistoryFiltersRequest;
import projectlx.co.zw.audittrail.utils.requests.AuditLogMultipleFiltersRequest;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

@CrossOrigin
@RestController
@RequestMapping("/ldms-audit-trail/v1/backoffice/audit-log")
@Tag(name = "Audit Log Backoffice Resource", description = "Admin console audit log operations (JWT only)")
@RequiredArgsConstructor
public class AuditLogBackofficeResource {

    private static final String BACKOFFICE = "BACKOFFICE";

    private final AuditLogProcessor auditLogProcessor;
    private static final Logger logger = LoggerFactory.getLogger(AuditLogBackofficeResource.class);

    @Auditable(action = "FIND_AUDIT_LOGS_BY_MULTIPLE_FILTERS")
    @PostMapping("/find-by-multiple-filters")
    @Operation(summary = "Find audit logs by multiple filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public AuditLogResponse findByMultipleFilters(
            @Valid @RequestBody AuditLogMultipleFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.findByMultipleFilters(request, locale, BACKOFFICE);
    }

    @Auditable(action = "EXPORT_AUDIT_LOGS")
    @PostMapping("/export")
    @Operation(summary = "Export audit logs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Export completed"),
            @ApiResponse(responseCode = "500", description = "Error during export")
    })
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestBody AuditLogMultipleFiltersRequest filters,
            @RequestParam String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return AuditLogExportResponseFactory.export(
                auditLogProcessor, filters, format, locale, BACKOFFICE, logger);
    }

    @Auditable(action = "FIND_AUDIT_LOG_BY_ID")
    @GetMapping("/find-by-id/{id}")
    @Operation(summary = "Find audit log by id")
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
        return auditLogProcessor.findById(id, locale, BACKOFFICE);
    }

    @Auditable(action = "FIND_AUDIT_LOGS_BY_TRACE")
    @GetMapping("/trace/{traceId}")
    @Operation(summary = "Find audit logs by trace id")
    public AuditLogResponse findByTrace(
            @PathVariable String traceId,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.findByTraceId(traceId, locale, BACKOFFICE);
    }

    @Auditable(action = "AUDIT_LOG_SERVICE_STATS")
    @GetMapping("/service/{serviceName}/stats")
    @Operation(summary = "Service audit statistics")
    public AuditLogResponse getServiceStats(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "24") int hours,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.getServiceStats(serviceName, hours, locale, BACKOFFICE);
    }

    @Auditable(action = "CHURN_OUT_AUDIT_LOGS")
    @PostMapping("/churn-out")
    @Operation(summary = "Churn out request logs")
    public AuditLogResponse churnOutRequestLogs(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.churnOutRequestLogs(locale, BACKOFFICE, "MANUAL");
    }

    @Auditable(action = "VIEW_CHURN_OUT_HISTORY")
    @GetMapping("/churn-history")
    @Operation(summary = "Find churn out history")
    public AuditLogResponse getChurnOutHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String triggerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triggeredBy,
            @RequestParam(required = false) String batchReference,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.getChurnOutHistory(
                page, size, triggerType, status, triggeredBy, batchReference, from, to, locale, BACKOFFICE);
    }

    @Auditable(action = "VIEW_CHURN_OUT_HISTORY")
    @PostMapping("/churn-history/find-by-multiple-filters")
    @Operation(summary = "Find churn out history by multiple filters")
    public AuditLogResponse findChurnOutHistoryByMultipleFilters(
            @Valid @RequestBody AuditLogChurnHistoryFiltersRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return auditLogProcessor.getChurnOutHistory(
                request.getPage(),
                request.getSize(),
                request.getTriggerType(),
                request.getStatus(),
                request.getTriggeredBy(),
                request.getBatchReference(),
                request.getFrom(),
                request.getTo(),
                locale,
                BACKOFFICE);
    }

    @Auditable(action = "VIEW_CHURN_OUT_HISTORY")
    @PostMapping("/churn-history/export")
    @Operation(summary = "Export churn out history")
    public ResponseEntity<byte[]> exportChurnOutHistory(
            @Valid @RequestBody AuditLogChurnHistoryFiltersRequest request,
            @RequestParam String format,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                    @RequestHeader(
                            value = Constants.LOCALE_LANGUAGE,
                            defaultValue = Constants.DEFAULT_LOCALE)
                    final Locale locale) {
        return AuditLogChurnHistoryExportResponseFactory.export(
                auditLogProcessor, request, format, locale, BACKOFFICE, logger);
    }
}
