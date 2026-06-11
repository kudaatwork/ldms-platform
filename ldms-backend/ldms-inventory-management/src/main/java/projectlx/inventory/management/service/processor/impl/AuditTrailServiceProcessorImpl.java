package projectlx.inventory.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.AuditTrailService;
import projectlx.inventory.management.service.processor.api.AuditTrailServiceProcessor;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;

@Service
@RequiredArgsConstructor
public class AuditTrailServiceProcessorImpl implements AuditTrailServiceProcessor {

    private final AuditTrailService auditTrailService;
    private static final Logger logger = LoggerFactory.getLogger(AuditTrailServiceProcessorImpl.class);

    @Override
    public void sendAuditLog(AuditLogDto auditLogDto) {
        logger.info("Incoming request to send audit log: {}", auditLogDto);

        auditTrailService.sendAuditLog(auditLogDto);

        logger.info("Audit log sent successfully");
    }
}