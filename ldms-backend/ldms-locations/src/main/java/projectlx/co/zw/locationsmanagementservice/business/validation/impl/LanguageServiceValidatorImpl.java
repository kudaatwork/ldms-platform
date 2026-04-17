package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.LanguageServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LanguageMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@RequiredArgsConstructor
public class LanguageServiceValidatorImpl implements LanguageServiceValidator {

    private final MessageService messageService;
    private Logger logger = LoggerFactory.getLogger(LanguageServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateLanguageRequestValid(CreateLanguageRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateLanguageRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_LANGUAGE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            logger.info("Validation failed: Language name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_LANGUAGE_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getIsoCode() == null || request.getIsoCode().isEmpty()) {
            logger.info("Validation failed: Language ISO code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_LANGUAGE_ISO_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getNativeName() == null || request.getNativeName().isEmpty()) {
            logger.info("Validation failed: Language native name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_LANGUAGE_NATIVE_NAME_MISSING.getCode(), new String[]{}, locale));
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
    public ValidatorDto isRequestValidForEditing(EditLanguageRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditLanguageRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_LANGUAGE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrLessThanOne(request.getId())) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            logger.info("Validation failed: Language name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_LANGUAGE_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getIsoCode() == null || request.getIsoCode().isEmpty()) {
            logger.info("Validation failed: Language ISO code is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_LANGUAGE_ISO_CODE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getNativeName() == null || request.getNativeName().isEmpty()) {
            logger.info("Validation failed: Language native name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_LANGUAGE_NATIVE_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(LanguageMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: LanguageMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_LANGUAGE_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
