package projectlx.user.management.service.business.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserPasswordServiceValidator;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.service.utils.requests.CreateUserPasswordRequest;
import projectlx.user.management.service.utils.requests.UserRoleMultipleFiltersRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPasswordServiceValidatorImpl implements UserPasswordServiceValidator {
    private static Logger logger = LoggerFactory.getLogger(UserPasswordServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateUserRequestValid(CreateUserPasswordRequest createUserPasswordRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (createUserPasswordRequest == null) {
            logger.info("Validation failed: CreateUserPasswordRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PASSWORD_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (createUserPasswordRequest.getPassword() == null || createUserPasswordRequest.getPassword().isEmpty()) {
            logger.info("Validation failed: Password is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PASSWORD_PASSWORD_MISSING.getCode(), new String[]{}, locale));
        }

        if (createUserPasswordRequest.getUserId() == null || createUserPasswordRequest.getUserId() < 1) {
            logger.info("Validation failed: User ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PASSWORD_USER_ID_INVALID.getCode(), new String[]{}, locale));
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

        if (input == null || input.isEmpty()) {
            logger.info("Validation failed: String is null or empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_STRING_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (id == null || id <= 0L) {
            logger.info("Validation failed: ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_ID_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(UserRoleMultipleFiltersRequest
                                                                          userRoleMultipleFiltersRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (userRoleMultipleFiltersRequest == null) {
            logger.info("Validation failed: UserRoleMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (userRoleMultipleFiltersRequest.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_MULTIPLE_FILTERS_PAGE_LESS_THAN_ZERO.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidForEditing(ChangeUserPasswordRequest changeUserPasswordRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (changeUserPasswordRequest == null) {
            logger.info("Validation failed: ChangeUserPasswordRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CHANGE_USER_PASSWORD_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (changeUserPasswordRequest.getPassword() == null || changeUserPasswordRequest.getPassword().isEmpty()) {
            logger.info("Validation failed: Password is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PASSWORD_PASSWORD_MISSING.getCode(), new String[]{}, locale));
        }

        if (changeUserPasswordRequest.getUserId() == null || changeUserPasswordRequest.getUserId() < 1) {
            logger.info("Validation failed: User ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PASSWORD_USER_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}
