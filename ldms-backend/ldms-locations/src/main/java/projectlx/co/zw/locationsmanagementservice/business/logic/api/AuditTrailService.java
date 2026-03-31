package projectlx.co.zw.locationsmanagementservice.business.logic.api;

import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;

public interface AuditTrailService {
    void sendAuditLog(AuditLogDto auditLogDto);
}
