package projectlx.co.zw.notificationsmanagementservice.business.validation.api;

import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

/**
 * Validator interface for AuditTrailService
 */
public interface AuditTrailServiceValidator {
    /**
     * Validates if the audit log DTO is valid for sending
     * @param auditLogDto The audit log DTO to validate
     * @param locale The locale for internationalization
     * @return ValidatorDto containing validation result and any error messages
     */
    ValidatorDto isAuditLogValid(AuditLogDto auditLogDto, Locale locale);
}