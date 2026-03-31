package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.LocalizedNameServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocalizedNameMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@Service
@RequiredArgsConstructor
public class LocalizedNameServiceValidatorImpl implements LocalizedNameServiceValidator {

    private final MessageService messageService;
    private Logger logger = LoggerFactory.getLogger(LocalizedNameServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateLocalizedNameRequestValid(CreateLocalizedNameRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateLocalizedNameRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_LOCALIZED_NAME_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getValue() == null || request.getValue().isEmpty()) {
            logger.info("Validation failed: Localized name value is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_LOCALIZED_NAME_VALUE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getLanguageId() == null) {
            logger.info("Validation failed: Language ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_LOCALIZED_NAME_LANGUAGE_ID_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getReferenceType() == null || request.getReferenceType().isEmpty()) {
            logger.info("Validation failed: Reference type is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_LOCALIZED_NAME_REFERENCE_TYPE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getReferenceId() == null) {
            logger.info("Validation failed: Reference ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_LOCALIZED_NAME_REFERENCE_ID_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (isNullOrLessThanOne(id)) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditLocalizedNameRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditLocalizedNameRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_LOCALIZED_NAME_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrLessThanOne(request.getId())) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getValue() == null || request.getValue().isEmpty()) {
            logger.info("Validation failed: Localized name value is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_LOCALIZED_NAME_VALUE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getLanguageId() == null) {
            logger.info("Validation failed: Language ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_LOCALIZED_NAME_LANGUAGE_ID_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getReferenceType() == null || request.getReferenceType().isEmpty()) {
            logger.info("Validation failed: Reference type is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_LOCALIZED_NAME_REFERENCE_TYPE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getReferenceId() == null) {
            logger.info("Validation failed: Reference ID is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_LOCALIZED_NAME_REFERENCE_ID_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveLocalizedNamesByMultipleFilters(LocalizedNameMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: LocalizedNameMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
