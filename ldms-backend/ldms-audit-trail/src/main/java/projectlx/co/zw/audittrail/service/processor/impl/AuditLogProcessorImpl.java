package projectlx.co.zw.audittrail.service.processor.impl;

import com.lowagie.text.DocumentException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogService;
import projectlx.co.zw.audittrail.service.processor.api.AuditLogProcessor;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogDto;
import projectlx.co.zw.audittrail.utils.requests.AuditLogMultipleFiltersRequest;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;

@RequiredArgsConstructor
public class AuditLogProcessorImpl implements AuditLogProcessor {

    private static final int EXPORT_PAGE_SIZE = 100;
    private static final int EXPORT_MAX_PAGES = 10_000;

    private final AuditLogService auditLogService;
    private final Logger logger = LoggerFactory.getLogger(AuditLogProcessorImpl.class);

    @Override
    public AuditLogResponse findByMultipleFilters(AuditLogMultipleFiltersRequest request, Locale locale, String username) {

        logger.info("Incoming audit log findByMultipleFilters from {}: {}", username, request);

        AuditLogResponse response = auditLogService.findByMultipleFilters(request, locale, username);

        logger.info(
                "Outgoing audit log findByMultipleFilters. statusCode={}, success={}",
                response.getStatusCode(),
                response.isSuccess());

        return response;
    }

    @Override
    public List<AuditLogDto> loadAllMatchingForExport(AuditLogMultipleFiltersRequest template, Locale locale, String username) {

        List<AuditLogDto> all = new ArrayList<>();
        int page = 0;
        while (page < EXPORT_MAX_PAGES) {
            AuditLogMultipleFiltersRequest pageRequest = copyForPage(template, page, EXPORT_PAGE_SIZE);
            AuditLogResponse response = auditLogService.findByMultipleFilters(pageRequest, locale, username);
            if (!response.isSuccess() || response.getAuditLogPage() == null) {
                break;
            }
            Page<AuditLogDto> dtoPage = response.getAuditLogPage();
            if (dtoPage.getContent().isEmpty()) {
                break;
            }
            all.addAll(dtoPage.getContent());
            if (!dtoPage.hasNext()) {
                break;
            }
            page++;
        }
        return all;
    }

    @Override
    public byte[] exportToCsv(List<AuditLogDto> items) {
        return auditLogService.exportToCsv(items);
    }

    @Override
    public byte[] exportToExcel(List<AuditLogDto> items) throws IOException {
        return auditLogService.exportToExcel(items);
    }

    @Override
    public byte[] exportToPdf(List<AuditLogDto> items) throws DocumentException {
        return auditLogService.exportToPdf(items);
    }

    @Override
    public AuditLogResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming audit log findById from {}: id={}", username, id);

        AuditLogResponse response = auditLogService.findById(id, locale, username);

        logger.info(
                "Outgoing audit log findById. statusCode={}, success={}",
                response.getStatusCode(),
                response.isSuccess());

        return response;
    }

    @Override
    public AuditLogResponse findByTraceId(String traceId, Locale locale, String username) {

        logger.info("Incoming audit log findByTraceId from {}: traceId={}", username, traceId);

        AuditLogResponse response = auditLogService.findByTraceId(traceId, locale, username);

        logger.info(
                "Outgoing audit log findByTraceId. statusCode={}, success={}",
                response.getStatusCode(),
                response.isSuccess());

        return response;
    }

    @Override
    public AuditLogResponse getServiceStats(String serviceName, int hours, Locale locale, String username) {

        logger.info("Incoming audit log stats from {}: serviceName={}, hours={}", username, serviceName, hours);

        AuditLogResponse response = auditLogService.getServiceStats(serviceName, hours, locale, username);

        logger.info(
                "Outgoing audit log stats. statusCode={}, success={}",
                response.getStatusCode(),
                response.isSuccess());

        return response;
    }

    private static AuditLogMultipleFiltersRequest copyForPage(AuditLogMultipleFiltersRequest src, int page, int size) {

        AuditLogMultipleFiltersRequest c = new AuditLogMultipleFiltersRequest();
        c.setServiceName(src.getServiceName());
        c.setUsername(src.getUsername());
        c.setEventType(src.getEventType());
        c.setHttpStatusCode(src.getHttpStatusCode());
        c.setFrom(src.getFrom());
        c.setTo(src.getTo());
        c.setSortBy(src.getSortBy());
        c.setSortDir(src.getSortDir());
        c.setSearchValue(src.getSearchValue());
        c.setAction(src.getAction());
        c.setRequestUrl(src.getRequestUrl());
        c.setHttpMethod(src.getHttpMethod());
        c.setTraceId(src.getTraceId());
        c.setPage(page);
        c.setSize(size);
        return c;
    }
}
