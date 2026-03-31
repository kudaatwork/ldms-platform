package projectlx.user.management.service.business.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserSecurityServiceValidator;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.service.utils.requests.EditUserSecurityRequest;
import projectlx.user.management.service.utils.requests.UserSecurityMultipleFiltersRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSecurityServiceValidatorImpl implements UserSecurityServiceValidator {
    private static Logger logger = LoggerFactory.getLogger(UserSecurityServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateUserSecurityRequestValid(CreateUserSecurityRequest createUserSecurityRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (createUserSecurityRequest == null) {
            logger.info("Validation failed: CreateUserSecurityRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_SECURITY_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (createUserSecurityRequest.getUserId() == null || createUserSecurityRequest.getUserId() < 1) {
            logger.info("Validation failed: User ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_SECURITY_USER_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (createUserSecurityRequest.getSecurityAnswer_1() == null || createUserSecurityRequest.getSecurityAnswer_1().isEmpty() ||
                createUserSecurityRequest.getSecurityAnswer_2() == null || createUserSecurityRequest.getSecurityAnswer_2().isEmpty() ||
                createUserSecurityRequest.getSecurityQuestion_1() == null || createUserSecurityRequest.getSecurityQuestion_1().isEmpty() ||
                createUserSecurityRequest.getTwoFactorAuthSecret() == null || createUserSecurityRequest.getTwoFactorAuthSecret().isEmpty()) {
            logger.info("Validation failed: One or more required security fields are missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_SECURITY_REQUIRED_FIELDS_MISSING.getCode(), new String[]{}, locale));
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
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditUserSecurityRequest editUserSecurityRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (editUserSecurityRequest == null) {
            logger.info("Validation failed: EditUserSecurityRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_SECURITY_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (editUserSecurityRequest.getId() == null || editUserSecurityRequest.getId() <= 0L) {
            logger.info("Validation failed: Security ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_SECURITY_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (editUserSecurityRequest.getUserId() == null || editUserSecurityRequest.getUserId() < 1) {
            logger.info("Validation failed: User ID is invalid for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_SECURITY_USER_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (editUserSecurityRequest.getSecurityAnswer_1() == null || editUserSecurityRequest.getSecurityAnswer_1().isEmpty() ||
                editUserSecurityRequest.getSecurityAnswer_2() == null || editUserSecurityRequest.getSecurityAnswer_2().isEmpty() ||
                editUserSecurityRequest.getSecurityQuestion_1() == null || editUserSecurityRequest.getSecurityQuestion_1().isEmpty() ||
                editUserSecurityRequest.getTwoFactorAuthSecret() == null || editUserSecurityRequest.getTwoFactorAuthSecret().isEmpty()) {
            logger.info("Validation failed: One or more required security fields are missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_SECURITY_REQUIRED_FIELDS_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveUserSecurityByMultipleFilters(UserSecurityMultipleFiltersRequest userSecurityMultipleFiltersRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (userSecurityMultipleFiltersRequest == null) {
            logger.info("Validation failed: UserSecurityMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (userSecurityMultipleFiltersRequest.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_MULTIPLE_FILTERS_PAGE_LESS_THAN_ZERO.getCode(), new String[]{}, locale));
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
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_STRING_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isBooleanValid(Boolean isTwoFactorEnabled, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (isTwoFactorEnabled == null) {
            logger.info("Validation failed: Boolean value is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_SECURITY_BOOLEAN_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
