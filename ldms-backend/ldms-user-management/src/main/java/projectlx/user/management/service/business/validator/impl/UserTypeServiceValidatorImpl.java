package projectlx.user.management.service.business.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserTypeServiceValidator;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.service.utils.requests.EditUserTypeRequest;
import projectlx.user.management.service.utils.requests.UserTypeMultipleFiltersRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserTypeServiceValidatorImpl implements UserTypeServiceValidator {
    private static Logger logger = LoggerFactory.getLogger(UserTypeServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateUserTypeRequestValid(CreateUserTypeRequest createUserTypeRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (createUserTypeRequest == null) {
            logger.info("Validation failed: CreateUserTypeRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_TYPE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (createUserTypeRequest.getUserTypeName() == null || createUserTypeRequest.getUserTypeName().isEmpty()) {
            logger.info("Validation failed: User type name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_TYPE_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (createUserTypeRequest.getDescription() == null || createUserTypeRequest.getDescription().isEmpty()) {
            logger.info("Validation failed: User type description is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_TYPE_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
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

        if (id == null || id <= 1L) {
            logger.info("Validation failed: ID is null or less than or equal to 1");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_TYPE_ID_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditUserTypeRequest editUserTypeRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (editUserTypeRequest == null) {
            logger.info("Validation failed: EditUserTypeRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_TYPE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (editUserTypeRequest.getId() == null || editUserTypeRequest.getId() <= 0L) {
            logger.info("Validation failed: User type ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_TYPE_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (editUserTypeRequest.getUserTypeName() == null || editUserTypeRequest.getUserTypeName().isEmpty()) {
            logger.info("Validation failed: User type name is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_TYPE_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (editUserTypeRequest.getDescription() == null || editUserTypeRequest.getDescription().isEmpty()) {
            logger.info("Validation failed: User type description is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_TYPE_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (userTypeMultipleFiltersRequest == null) {
            logger.info("Validation failed: UserTypeMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_TYPE_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (userTypeMultipleFiltersRequest.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_TYPE_MULTIPLE_FILTERS_PAGE_LESS_THAN_ZERO.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isStringValid(String input, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            logger.info("Validation failed: String is null or empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_TYPE_STRING_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
