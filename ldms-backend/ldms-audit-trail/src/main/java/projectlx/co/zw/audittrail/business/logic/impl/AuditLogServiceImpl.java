package projectlx.co.zw.audittrail.business.logic.impl;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogQueryService;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogService;
import projectlx.co.zw.audittrail.business.validation.api.AuditLogQueryValidator;
import projectlx.co.zw.audittrail.model.AuditLog;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogDto;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogFilter;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogServiceStats;
import projectlx.co.zw.audittrail.utils.enums.I18Code;
import projectlx.co.zw.audittrail.utils.mapper.AuditLogDtoMapper;
import projectlx.co.zw.audittrail.utils.requests.AuditLogSearchRequest;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogQueryService queryService;
    private final AuditLogQueryValidator validator;
    private final MessageService messageService;

    @Override
    public AuditLogResponse search(AuditLogSearchRequest request, Locale locale, String username) {

        AuditLogFilter filter = toFilter(request);

        ValidatorDto validationResult = validator.validateSearch(filter, locale);

        if (!Boolean.TRUE.equals(validationResult.getSuccess())) {

            String responseMessage = messageService.getMessage(I18Code.AUDIT_LOG_REQUEST_INVALID.getCode(),
                    new String[] {}, locale);

            return buildWithErrors(
                    400,
                    false,
                    responseMessage,
                    null,
                    null,
                    null,
                    null,
                    validationResult.getErrorMessages());
        }

        Page<AuditLog> result = queryService.search(filter);
        Page<AuditLogDto> dtoPage = toDtoPage(result, false);

        String responseMessage =
                messageService.getMessage(I18Code.AUDIT_LOG_SEARCH_SUCCESS.getCode(), new String[] {}, locale);

        return build(200, true, responseMessage, null, null, dtoPage,
                null);
    }

    @Override
    public AuditLogResponse findById(Long id, Locale locale, String username) {

        ValidatorDto validationResult = validator.validateId(id, locale);

        if (!Boolean.TRUE.equals(validationResult.getSuccess())) {

            String responseMessage = messageService.getMessage(I18Code.AUDIT_LOG_REQUEST_INVALID.getCode(),
                    new String[] {}, locale);

            return buildWithErrors(
                    400,
                    false,
                    responseMessage,
                    null,
                    null,
                    null,
                    null,
                    validationResult.getErrorMessages());
        }

        return queryService.findById(id).map(log -> {String responseMessage = messageService.getMessage(
                            I18Code.AUDIT_LOG_RETRIEVED_SUCCESS.getCode(), new String[] {}, locale);
                    return build(200, true, responseMessage, AuditLogDtoMapper.toDto(log,
                            true), null, null, null);
                }).orElseGet(() -> {String responseMessage = messageService.getMessage(I18Code.AUDIT_LOG_NOT_FOUND.getCode(),
                new String[] {}, locale);
                    return build(404, false, responseMessage, null, null, null, null);
                });
    }

    @Override
    public AuditLogResponse findByTraceId(String traceId, Locale locale, String username) {

        ValidatorDto validationResult = validator.validateTraceId(traceId, locale);

        if (!Boolean.TRUE.equals(validationResult.getSuccess())) {

            String responseMessage =
                    messageService.getMessage(I18Code.AUDIT_LOG_REQUEST_INVALID.getCode(), new String[] {}, locale);

            return buildWithErrors(
                    400,
                    false,
                    responseMessage,
                    null,
                    null,
                    null,
                    null,
                    validationResult.getErrorMessages());
        }

        List<AuditLogDto> dtos =
                queryService.findByTraceIdOrdered(traceId).stream().map(e -> AuditLogDtoMapper.toDto(e,
                        true)).toList();

        String responseMessage = messageService.getMessage(
                I18Code.AUDIT_LOG_TRACE_RETRIEVED_SUCCESS.getCode(), new String[] {}, locale);

        return build(200, true, responseMessage, null, dtos, null, null);
    }

    @Override
    public AuditLogResponse getServiceStats(String serviceName, int hours, Locale locale, String username) {

        ValidatorDto validationResult = validator.validateServiceStats(serviceName, hours, locale);

        if (!Boolean.TRUE.equals(validationResult.getSuccess())) {

            String responseMessage =
                    messageService.getMessage(I18Code.AUDIT_LOG_REQUEST_INVALID.getCode(), new String[] {}, locale);

            return buildWithErrors(
                    400,
                    false,
                    responseMessage,
                    null,
                    null,
                    null,
                    null,
                    validationResult.getErrorMessages());
        }

        String responseMessage = messageService.getMessage(
                I18Code.AUDIT_LOG_STATS_RETRIEVED_SUCCESS.getCode(), new String[] {}, locale);

        return build(
                200,
                true,
                responseMessage,
                null,
                null,
                null,
                queryService.buildServiceStats(serviceName.trim(), hours));
    }

    private static AuditLogFilter toFilter(AuditLogSearchRequest searchRequest) {

        int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;

        int size = searchRequest.getSize() != null ? searchRequest.getSize() : 20;

        String sortBy = searchRequest.getSortBy() != null && !searchRequest.getSortBy().isBlank()
                ? searchRequest.getSortBy().trim()
                : "requestTimestamp";

        String sortDir = searchRequest.getSortDir() != null && !searchRequest.getSortDir().isBlank()
                ? searchRequest.getSortDir().trim()
                : "DESC";

        return new AuditLogFilter(
                searchRequest.getServiceName(),
                searchRequest.getUsername(),
                searchRequest.getEventType(),
                searchRequest.getHttpStatusCode(),
                searchRequest.getFrom(),
                searchRequest.getTo(),
                page,
                size,
                sortBy,
                sortDir);
    }

    private static Page<AuditLogDto> toDtoPage(Page<AuditLog> page, boolean includeLargePayloads) {

        List<AuditLogDto> content =
                page.getContent().stream().map(e -> AuditLogDtoMapper.toDto(e, includeLargePayloads)).toList();

        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    private static AuditLogResponse build(

            int statusCode,
            boolean success,
            String message,
            AuditLogDto auditLog,
            List<AuditLogDto> auditLogList,
            Page<AuditLogDto> auditLogPage,
            AuditLogServiceStats serviceStats) {
        AuditLogResponse auditLogResponse = new AuditLogResponse();
        auditLogResponse.setStatusCode(statusCode);
        auditLogResponse.setSuccess(success);
        auditLogResponse.setMessage(message);
        auditLogResponse.setAuditLog(auditLog);
        auditLogResponse.setAuditLogList(auditLogList);
        auditLogResponse.setAuditLogPage(auditLogPage);
        auditLogResponse.setServiceStats(serviceStats);
        return auditLogResponse;
    }

    private static AuditLogResponse buildWithErrors(

            int statusCode,
            boolean success,
            String message,
            AuditLogDto auditLog,
            List<AuditLogDto> auditLogList,
            Page<AuditLogDto> auditLogPage,
            AuditLogServiceStats serviceStats,
            List<String> errorMessages) {
        AuditLogResponse auditLogResponse = build(statusCode, success, message, auditLog, auditLogList, auditLogPage,
                serviceStats);
        auditLogResponse.setErrorMessages(errorMessages);

        return auditLogResponse;
    }
}
