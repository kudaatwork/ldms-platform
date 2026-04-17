package projectlx.co.zw.audittrail.service.processor.impl;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.audittrail.business.logic.api.AuditLogService;
import projectlx.co.zw.audittrail.service.processor.api.AuditLogProcessor;
import projectlx.co.zw.audittrail.utils.requests.AuditLogSearchRequest;
import projectlx.co.zw.audittrail.utils.responses.AuditLogResponse;

@RequiredArgsConstructor
public class AuditLogProcessorImpl implements AuditLogProcessor {

    private final AuditLogService auditLogService;
    private final Logger logger = LoggerFactory.getLogger(AuditLogProcessorImpl.class);

    @Override
    public AuditLogResponse search(AuditLogSearchRequest request, Locale locale, String username) {

        logger.info("Incoming audit log search request from {}: {}", username, request);

        AuditLogResponse response = auditLogService.search(request, locale, username);

        logger.info(
                "Outgoing audit log search response. statusCode={}, success={}",
                response.getStatusCode(),
                response.isSuccess());

        return response;
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
}
