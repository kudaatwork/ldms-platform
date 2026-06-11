package projectlx.inventory.management.service.processor.api;

import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;

public interface AuditTrailServiceProcessor {
    void sendAuditLog(AuditLogDto auditLogDto);
}