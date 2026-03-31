package projectlx.co.zw.audittrail.processor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import projectlx.co.zw.audittrail.api.ApiResponse;
import projectlx.co.zw.audittrail.dto.AuditLogDto;
import projectlx.co.zw.audittrail.dto.AuditLogFilter;
import projectlx.co.zw.audittrail.dto.AuditLogServiceStats;
import projectlx.co.zw.audittrail.entity.AuditLog;
import projectlx.co.zw.audittrail.mapper.AuditLogDtoMapper;
import projectlx.co.zw.audittrail.service.AuditLogQueryService;
import projectlx.co.zw.audittrail.validator.AuditLogQueryValidator;
import projectlx.co.zw.shared_library.utils.dtos.PaginatedResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

@Component
public class AuditLogServiceProcessor {

    private final AuditLogQueryValidator validator;
    private final AuditLogQueryService service;

    public AuditLogServiceProcessor(AuditLogQueryValidator validator, AuditLogQueryService service) {
        this.validator = validator;
        this.service = service;
    }

    public ResponseEntity<ApiResponse<PaginatedResponse<AuditLogDto>>> search(
            String serviceName,
            String username,
            String eventType,
            Integer httpStatusCode,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        AuditLogFilter filter =
                new AuditLogFilter(serviceName, username, eventType, httpStatusCode, from, to, page, size, sortBy, sortDir);
        ValidatorDto vd = validator.validateSearch(filter);
        if (!vd.success) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(joinErrors(vd)));
        }

        Page<AuditLog> result = service.search(filter);
        List<AuditLogDto> dtos =
                result.getContent().stream().map(e -> AuditLogDtoMapper.toDto(e, false)).toList();
        PaginatedResponse<AuditLogDto> body =
                new PaginatedResponse<>(dtos, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    public ResponseEntity<ApiResponse<AuditLogDto>> getById(Long id) {
        ValidatorDto vd = validator.validateId(id);
        if (!vd.success) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(joinErrors(vd)));
        }
        return service
                .findById(id)
                .map(log -> ResponseEntity.ok(ApiResponse.ok(AuditLogDtoMapper.toDto(log, true))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Audit log not found")));
    }

    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getByTrace(String traceId) {
        ValidatorDto vd = validator.validateTraceId(traceId);
        if (!vd.success) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(joinErrors(vd)));
        }
        List<AuditLogDto> dtos = service.findByTraceIdOrdered(traceId).stream()
                .map(e -> AuditLogDtoMapper.toDto(e, true))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    public ResponseEntity<ApiResponse<AuditLogServiceStats>> getServiceStats(String serviceName, int hours) {
        ValidatorDto vd = validator.validateServiceStats(serviceName, hours);
        if (!vd.success) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(joinErrors(vd)));
        }
        AuditLogServiceStats stats = service.buildServiceStats(serviceName.trim(), hours);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    private static String joinErrors(ValidatorDto vd) {
        return vd.errorMessages.stream().map(Object::toString).collect(Collectors.joining("; "));
    }
}
