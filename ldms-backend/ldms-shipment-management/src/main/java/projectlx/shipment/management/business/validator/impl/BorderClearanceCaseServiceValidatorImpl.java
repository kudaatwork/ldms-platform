package projectlx.shipment.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.shipment.management.business.validator.api.BorderClearanceCaseServiceValidator;
import projectlx.shipment.management.utils.enums.BorderClearanceDocumentType;
import projectlx.shipment.management.utils.enums.I18Code;
import projectlx.shipment.management.utils.requests.AddBorderClearanceDocumentRequest;
import projectlx.shipment.management.utils.requests.BorderClearanceMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.ClearBorderCaseRequest;
import projectlx.shipment.management.utils.requests.RejectBorderCaseRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class BorderClearanceCaseServiceValidatorImpl implements BorderClearanceCaseServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(BorderClearanceCaseServiceValidatorImpl.class);
    private final MessageService messageService;

    @Override
    public ValidatorDto isAddDocumentRequestValid(AddBorderClearanceDocumentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: AddBorderClearanceDocumentRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getCaseId() == null || request.getCaseId() < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"caseId"}, locale));
        }
        if (request.getFileUploadId() == null || request.getFileUploadId() < 1) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_FILE_UPLOAD_REQUIRED.getCode(), new String[]{}, locale));
        }
        if (request.getDocumentType() == null || request.getDocumentType().isBlank()) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_BORDER_CLEARANCE_DOCUMENT_TYPE_REQUIRED.getCode(), new String[]{}, locale));
        } else {
            try {
                BorderClearanceDocumentType.valueOf(request.getDocumentType().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_BORDER_CLEARANCE_DOCUMENT_TYPE_INVALID.getCode(), new String[]{}, locale));
            }
        }
        if (request.getFileName() == null || request.getFileName().isBlank()) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"fileName"}, locale));
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isClearBorderCaseRequestValid(Long caseId, ClearBorderCaseRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (caseId == null || caseId < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"id"}, locale));
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isRejectBorderCaseRequestValid(Long caseId, RejectBorderCaseRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (caseId == null || caseId < 1) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"id"}, locale));
        }

        return buildResult(errors);
    }

    @Override
    public ValidatorDto isMultipleFiltersRequestValid(BorderClearanceMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            return new ValidatorDto(true, null, new ArrayList<>());
        }
        if (request.getOrganizationId() != null && request.getOrganizationId() < 1) {
            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"organizationId"}, locale));
        }

        return buildResult(errors);
    }

    private ValidatorDto buildResult(List<String> errors) {
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, new ArrayList<>());
        }
        return new ValidatorDto(false, null, errors);
    }
}
