package projectlx.co.zw.notifications.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.notifications.business.validation.api.AuditTrailServiceValidator;
import projectlx.co.zw.notifications.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.dtos.AuditLogDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class AuditTrailServiceValidatorImpl implements AuditTrailServiceValidator {

    private final MessageService messageService;
    private static final Logger logger = LoggerFactory.getLogger(AuditTrailServiceValidatorImpl.class);

    @Override
    public ValidatorDto isAuditLogValid(AuditLogDto auditLogDto, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (auditLogDto == null) {
            logger.info("Validation failed: AuditLogDto is null");
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (auditLogDto.getAction() == null || auditLogDto.getAction().isEmpty()) {
            logger.info("Validation failed: Action is missing");
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_ACTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (auditLogDto.getUsername() == null || auditLogDto.getUsername().isEmpty()) {
            logger.info("Validation failed: Username is missing");
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_USERNAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (auditLogDto.getEventType() == null) {
            logger.info("Validation failed: Event type is missing");
            errors.add(messageService.getMessage(I18Code.AUDIT_LOG_EVENT_TYPE_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}
