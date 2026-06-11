package projectlx.fleet.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.fleet.management.business.validator.api.FleetComplianceServiceValidator;
import projectlx.fleet.management.utils.enums.ComplianceStatus;
import projectlx.fleet.management.utils.enums.ComplianceSubjectType;
import projectlx.fleet.management.utils.enums.ComplianceType;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.requests.CreateFleetComplianceRecordRequest;
import projectlx.fleet.management.utils.requests.EditFleetComplianceRecordRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FleetComplianceServiceValidatorImpl implements FleetComplianceServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(FleetComplianceServiceValidatorImpl.class);
    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateFleetComplianceRequestValid(CreateFleetComplianceRecordRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: create compliance request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        validateSubject(request.getSubjectType(), request.getSubjectId(), errors, locale);
        validateComplianceType(request.getComplianceType(), errors, locale);
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isEditFleetComplianceRequestValid(EditFleetComplianceRecordRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            logger.info("Validation failed: edit compliance request is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        if (request.getId() == null || request.getId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"id"}, locale));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                ComplianceStatus.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"status"}, locale));
            }
        }
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    private void validateSubject(String subjectType, Long subjectId, List<String> errors, Locale locale) {
        if (subjectType == null || subjectType.isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"subjectType"}, locale));
        } else {
            try {
                ComplianceSubjectType.valueOf(subjectType.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"subjectType"}, locale));
            }
        }
        if (subjectId == null || subjectId < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"subjectId"}, locale));
        }
    }

    private void validateComplianceType(String complianceType, List<String> errors, Locale locale) {
        if (complianceType == null || complianceType.isBlank()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"complianceType"}, locale));
            return;
        }
        try {
            ComplianceType.valueOf(complianceType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"complianceType"}, locale));
        }
    }
}
