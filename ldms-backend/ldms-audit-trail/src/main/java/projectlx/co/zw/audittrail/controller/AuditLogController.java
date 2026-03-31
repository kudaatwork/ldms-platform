package projectlx.co.zw.audittrail.controller;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.audittrail.api.ApiResponse;
import projectlx.co.zw.audittrail.dto.AuditLogDto;
import projectlx.co.zw.audittrail.dto.AuditLogServiceStats;
import projectlx.co.zw.audittrail.processor.AuditLogServiceProcessor;
import projectlx.co.zw.shared_library.utils.dtos.PaginatedResponse;

@RestController
@RequestMapping("/api/v1/system/audit")
public class AuditLogController {

    private final AuditLogServiceProcessor serviceProcessor;

    public AuditLogController(AuditLogServiceProcessor serviceProcessor) {
        this.serviceProcessor = serviceProcessor;
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PaginatedResponse<AuditLogDto>>> search(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Integer httpStatusCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "requestTimestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        return serviceProcessor.search(
                serviceName, username, eventType, httpStatusCode, from, to, page, size, sortBy, sortDir);
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<ApiResponse<AuditLogDto>> getById(@PathVariable Long id) {
        return serviceProcessor.getById(id);
    }

    @GetMapping("/logs/trace/{traceId}")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getByTrace(@PathVariable String traceId) {
        return serviceProcessor.getByTrace(traceId);
    }

    @GetMapping("/logs/service/{serviceName}/stats")
    public ResponseEntity<ApiResponse<AuditLogServiceStats>> getServiceStats(
            @PathVariable String serviceName, @RequestParam(defaultValue = "24") int hours) {
        return serviceProcessor.getServiceStats(serviceName, hours);
    }
}
