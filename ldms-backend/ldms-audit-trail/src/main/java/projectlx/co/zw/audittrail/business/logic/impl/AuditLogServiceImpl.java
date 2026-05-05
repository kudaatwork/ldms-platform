package projectlx.co.zw.audittrail.business.logic.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogQueryService;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogService;
import projectlx.co.zw.audittrail.business.validation.api.AuditLogQueryValidator;
import projectlx.co.zw.audittrail.model.AuditLog;
import projectlx.co.zw.audittrail.model.AuditLogChurnHistory;
import projectlx.co.zw.audittrail.repository.AuditLogChurnHistoryRepository;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogChurnHistoryDto;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogDto;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogFilter;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogServiceStats;
import projectlx.co.zw.audittrail.utils.enums.AuditLogChurnStatus;
import projectlx.co.zw.audittrail.utils.enums.I18Code;
import projectlx.co.zw.audittrail.utils.mapper.AuditLogDtoMapper;
import projectlx.co.zw.audittrail.utils.requests.AuditLogChurnHistoryFiltersRequest;
import projectlx.co.zw.audittrail.utils.requests.AuditLogMultipleFiltersRequest;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private static final String[] EXPORT_HEADERS = {
        "id",
        "action",
        "serviceName",
        "username",
        "eventType",
        "httpMethod",
        "httpStatusCode",
        "requestTimestamp",
        "traceId",
        "responseTimeMs",
        "clientIpAddress"
    };
    private static final String[] CHURN_HISTORY_EXPORT_HEADERS = {
        "id",
        "batchReference",
        "triggerType",
        "triggeredBy",
        "triggeredAt",
        "deletedLogCount",
        "churnStatus",
        "jobExecutionId",
        "completedAt",
        "failureReason"
    };

    private final AuditLogQueryService queryService;
    private final AuditLogQueryValidator validator;
    private final MessageService messageService;
    private final AuditLogChurnLaunchService churnLaunchService;
    private final AuditLogChurnHistoryRepository churnHistoryRepository;

    @Override
    public AuditLogResponse findByMultipleFilters(AuditLogMultipleFiltersRequest request, Locale locale, String username) {

        if (request == null) {

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
                    List.of(responseMessage));
        }

        ValidatorDto pageValidator = validator.validateMultipleFiltersRequest(request, locale);
        if (!Boolean.TRUE.equals(pageValidator.getSuccess())) {

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
                    pageValidator.getErrorMessages());
        }

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

    @Override
    public AuditLogResponse churnOutRequestLogs(Locale locale, String username, String triggerType) {
        return churnLaunchService.launch(locale, username, triggerType);
    }

    @Override
    public AuditLogResponse getChurnOutHistory(int page, int size, Locale locale, String username) {
        return getChurnOutHistory(page, size, null, null, null, null, null, null, locale, username);
    }

    @Override
    public AuditLogResponse getChurnOutHistory(
            int page,
            int size,
            String triggerType,
            String status,
            String triggeredBy,
            String batchReference,
            LocalDateTime from,
            LocalDateTime to,
            Locale locale,
            String username) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        Page<AuditLogChurnHistory> historyPage = churnHistoryRepository.search(
                trimToNull(triggerType),
                parseChurnStatus(status),
                trimToNull(triggeredBy),
                trimToNull(batchReference),
                from,
                to,
                PageRequest.of(safePage, safeSize));
        Page<AuditLogChurnHistoryDto> dtoPage = historyPage.map(AuditLogServiceImpl::toChurnHistoryDto);
        String responseMessage = messageService.getMessage(
                I18Code.AUDIT_LOG_RETRIEVED_SUCCESS.getCode(), new String[] {}, locale);
        AuditLogResponse response = build(200, true, responseMessage, null, null, null, null);
        response.setChurnHistoryPage(dtoPage);
        return response;
    }

    @Override
    public List<AuditLogChurnHistoryDto> loadAllMatchingChurnHistory(
            AuditLogChurnHistoryFiltersRequest request, Locale locale, String username) {
        int size = request.getSize() <= 0 ? 1000 : Math.min(request.getSize(), 5000);
        Page<AuditLogChurnHistory> historyPage = churnHistoryRepository.search(
                trimToNull(request.getTriggerType()),
                parseChurnStatus(request.getStatus()),
                trimToNull(request.getTriggeredBy()),
                trimToNull(request.getBatchReference()),
                request.getFrom(),
                request.getTo(),
                PageRequest.of(Math.max(0, request.getPage()), size));
        return historyPage.map(AuditLogServiceImpl::toChurnHistoryDto).getContent();
    }

    @Override
    public byte[] exportToCsv(List<AuditLogDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", EXPORT_HEADERS)).append("\n");

        for (AuditLogDto log : items) {
            sb.append(log.id()).append(",")
                    .append(safeCsv(log.action())).append(",")
                    .append(safeCsv(log.serviceName())).append(",")
                    .append(safeCsv(log.username())).append(",")
                    .append(log.eventType() != null ? log.eventType().name() : "").append(",")
                    .append(safeCsv(log.httpMethod())).append(",")
                    .append(log.httpStatusCode() != null ? log.httpStatusCode() : "").append(",")
                    .append(log.requestTimestamp() != null ? log.requestTimestamp().toString() : "").append(",")
                    .append(safeCsv(log.traceId())).append(",")
                    .append(log.responseTimeMs() != null ? log.responseTimeMs() : "").append(",")
                    .append(safeCsv(log.clientIpAddress()))
                    .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<AuditLogDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Audit logs");

        Row header = sheet.createRow(0);
        for (int i = 0; i < EXPORT_HEADERS.length; i++) {
            header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
        }

        int rowIdx = 1;
        for (AuditLogDto log : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(log.id() != null ? log.id() : 0L);
            row.createCell(1).setCellValue(safeCsv(log.action()));
            row.createCell(2).setCellValue(safeCsv(log.serviceName()));
            row.createCell(3).setCellValue(safeCsv(log.username()));
            row.createCell(4).setCellValue(log.eventType() != null ? log.eventType().name() : "");
            row.createCell(5).setCellValue(safeCsv(log.httpMethod()));
            row.createCell(6).setCellValue(log.httpStatusCode() != null ? log.httpStatusCode() : 0);
            row.createCell(7).setCellValue(log.requestTimestamp() != null ? log.requestTimestamp().toString() : "");
            row.createCell(8).setCellValue(safeCsv(log.traceId()));
            row.createCell(9).setCellValue(log.responseTimeMs() != null ? log.responseTimeMs() : 0L);
            row.createCell(10).setCellValue(safeCsv(log.clientIpAddress()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<AuditLogDto> items) throws DocumentException {

        com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        com.lowagie.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        document.add(new Paragraph("AUDIT LOG EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(EXPORT_HEADERS.length);
        for (String h : EXPORT_HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (AuditLogDto log : items) {
            table.addCell(new Phrase(String.valueOf(log.id()), cellFont));
            table.addCell(new Phrase(safeCsv(log.action()), cellFont));
            table.addCell(new Phrase(safeCsv(log.serviceName()), cellFont));
            table.addCell(new Phrase(safeCsv(log.username()), cellFont));
            table.addCell(new Phrase(log.eventType() != null ? log.eventType().name() : "", cellFont));
            table.addCell(new Phrase(safeCsv(log.httpMethod()), cellFont));
            table.addCell(new Phrase(log.httpStatusCode() != null ? String.valueOf(log.httpStatusCode()) : "", cellFont));
            table.addCell(new Phrase(log.requestTimestamp() != null ? log.requestTimestamp().toString() : "", cellFont));
            table.addCell(new Phrase(safeCsv(log.traceId()), cellFont));
            table.addCell(new Phrase(log.responseTimeMs() != null ? String.valueOf(log.responseTimeMs()) : "", cellFont));
            table.addCell(new Phrase(safeCsv(log.clientIpAddress()), cellFont));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportChurnHistoryToCsv(List<AuditLogChurnHistoryDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", CHURN_HISTORY_EXPORT_HEADERS)).append("\n");
        for (AuditLogChurnHistoryDto row : items) {
            sb.append(row.id()).append(",")
                    .append(safeCsv(row.batchReference())).append(",")
                    .append(safeCsv(row.triggerType())).append(",")
                    .append(safeCsv(row.triggeredBy())).append(",")
                    .append(row.triggeredAt() != null ? row.triggeredAt() : "").append(",")
                    .append(row.deletedLogCount() != null ? row.deletedLogCount() : 0).append(",")
                    .append(safeCsv(row.churnStatus())).append(",")
                    .append(row.jobExecutionId() != null ? row.jobExecutionId() : "").append(",")
                    .append(row.completedAt() != null ? row.completedAt() : "").append(",")
                    .append(safeCsv(row.failureReason()))
                    .append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportChurnHistoryToExcel(List<AuditLogChurnHistoryDto> items) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Churn history");
        Row header = sheet.createRow(0);
        for (int i = 0; i < CHURN_HISTORY_EXPORT_HEADERS.length; i++) {
            header.createCell(i).setCellValue(CHURN_HISTORY_EXPORT_HEADERS[i]);
        }

        int rowIdx = 1;
        for (AuditLogChurnHistoryDto item : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(item.id() != null ? item.id() : 0L);
            row.createCell(1).setCellValue(safeCsv(item.batchReference()));
            row.createCell(2).setCellValue(safeCsv(item.triggerType()));
            row.createCell(3).setCellValue(safeCsv(item.triggeredBy()));
            row.createCell(4).setCellValue(item.triggeredAt() != null ? item.triggeredAt().toString() : "");
            row.createCell(5).setCellValue(item.deletedLogCount() != null ? item.deletedLogCount() : 0L);
            row.createCell(6).setCellValue(safeCsv(item.churnStatus()));
            row.createCell(7).setCellValue(item.jobExecutionId() != null ? item.jobExecutionId() : 0L);
            row.createCell(8).setCellValue(item.completedAt() != null ? item.completedAt().toString() : "");
            row.createCell(9).setCellValue(safeCsv(item.failureReason()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportChurnHistoryToPdf(List<AuditLogChurnHistoryDto> items) throws DocumentException {
        com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        com.lowagie.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        document.add(new Paragraph("AUDIT LOG CHURN HISTORY EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(CHURN_HISTORY_EXPORT_HEADERS.length);
        for (String h : CHURN_HISTORY_EXPORT_HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (AuditLogChurnHistoryDto item : items) {
            table.addCell(new Phrase(item.id() != null ? String.valueOf(item.id()) : "", cellFont));
            table.addCell(new Phrase(safeCsv(item.batchReference()), cellFont));
            table.addCell(new Phrase(safeCsv(item.triggerType()), cellFont));
            table.addCell(new Phrase(safeCsv(item.triggeredBy()), cellFont));
            table.addCell(new Phrase(item.triggeredAt() != null ? item.triggeredAt().toString() : "", cellFont));
            table.addCell(new Phrase(item.deletedLogCount() != null ? String.valueOf(item.deletedLogCount()) : "", cellFont));
            table.addCell(new Phrase(safeCsv(item.churnStatus()), cellFont));
            table.addCell(new Phrase(item.jobExecutionId() != null ? String.valueOf(item.jobExecutionId()) : "", cellFont));
            table.addCell(new Phrase(item.completedAt() != null ? item.completedAt().toString() : "", cellFont));
            table.addCell(new Phrase(safeCsv(item.failureReason()), cellFont));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private static String safeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", " ").replace("\n", " ").replace("\r", " ");
    }

    private static AuditLogFilter toFilter(AuditLogMultipleFiltersRequest searchRequest) {

        int page = searchRequest.getPage();
        if (page < 0) {
            page = 0;
        }

        int size = searchRequest.getSize();
        if (size <= 0) {
            size = 20;
        }

        String sortBy = searchRequest.getSortBy() != null && !searchRequest.getSortBy().isBlank()
                ? searchRequest.getSortBy().trim()
                : "requestTimestamp";

        String sortDir = searchRequest.getSortDir() != null && !searchRequest.getSortDir().isBlank()
                ? searchRequest.getSortDir().trim()
                : "DESC";

        String searchValue = searchRequest.getSearchValue();
        if (searchValue != null) {
            searchValue = searchValue.isBlank() ? null : searchValue.trim();
        }

        return new AuditLogFilter(
                searchRequest.getServiceName(),
                searchRequest.getUsername(),
                searchRequest.getEventType(),
                searchRequest.getHttpStatusCode(),
                searchRequest.getFrom(),
                searchRequest.getTo(),
                searchValue,
                trimToNull(searchRequest.getAction()),
                trimToNull(searchRequest.getRequestUrl()),
                trimToNull(searchRequest.getHttpMethod()),
                trimToNull(searchRequest.getTraceId()),
                page,
                size,
                sortBy,
                sortDir);
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static AuditLogChurnStatus parseChurnStatus(String status) {
        String safeStatus = trimToNull(status);
        if (safeStatus == null) {
            return null;
        }
        try {
            return AuditLogChurnStatus.valueOf(safeStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    private static AuditLogChurnHistoryDto toChurnHistoryDto(AuditLogChurnHistory h) {
        return new AuditLogChurnHistoryDto(
                h.getId(),
                h.getBatchReference(),
                h.getTriggerType(),
                h.getTriggeredBy(),
                h.getTriggeredAt(),
                h.getDeletedLogCount(),
                h.getOldestRequestTimestamp(),
                h.getNewestRequestTimestamp(),
                h.getChurnStatus() != null ? h.getChurnStatus().name() : null,
                h.getJobExecutionId(),
                h.getFailureReason(),
                h.getCompletedAt());
    }
}
