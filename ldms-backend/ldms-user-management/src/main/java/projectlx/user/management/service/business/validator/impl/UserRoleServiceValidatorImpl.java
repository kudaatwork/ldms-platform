package projectlx.user.management.service.business.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserRoleServiceValidator;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.CreateUserRoleRequest;
import projectlx.user.management.service.utils.requests.EditUserRoleRequest;
import projectlx.user.management.service.utils.requests.UserRoleMultipleFiltersRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;

import static projectlx.user.management.service.business.validator.impl.UserServiceValidatorImpl.doesStringHaveDigit;

@RequiredArgsConstructor
public class UserRoleServiceValidatorImpl implements UserRoleServiceValidator {
    private static Logger logger = LoggerFactory.getLogger(UserRoleServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateUserRoleRequestValid(CreateUserRoleRequest createUserRoleRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (createUserRoleRequest == null) {
            logger.info("Validation failed: CreateUserRoleRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ROLE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (createUserRoleRequest.getRole() == null || createUserRoleRequest.getRole().isEmpty()) {
            logger.info("Validation failed: Role name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ROLE_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (createUserRoleRequest.getDescription() == null || createUserRoleRequest.getDescription().isEmpty()) {
            logger.info("Validation failed: Role description is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ROLE_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (createUserRoleRequest.getRole() != null && doesStringHaveDigit(createUserRoleRequest.getRole())) {
            logger.info("Validation failed: Role name contains digits");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ROLE_NAME_CONTAINS_DIGITS.getCode(), new String[]{}, locale));
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
    public ValidatorDto isRequestValidForEditing(EditUserRoleRequest editUserRoleRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (editUserRoleRequest == null) {
            logger.info("Validation failed: EditUserRoleRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_ROLE_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (editUserRoleRequest.getId() == null || editUserRoleRequest.getId() <= 0L) {
            logger.info("Validation failed: Role ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_ROLE_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (editUserRoleRequest.getRole() == null || editUserRoleRequest.getRole().isEmpty()) {
            logger.info("Validation failed: Role name is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_ROLE_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (editUserRoleRequest.getDescription() == null || editUserRoleRequest.getDescription().isEmpty()) {
            logger.info("Validation failed: Role description is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_ROLE_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (editUserRoleRequest.getRole() != null && doesStringHaveDigit(editUserRoleRequest.getRole())) {
            logger.info("Validation failed: Role name contains digits for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_ROLE_NAME_CONTAINS_DIGITS.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveUserRoleByMultipleFilters(UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (userRoleMultipleFiltersRequest == null) {
            logger.info("Validation failed: UserRoleMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ROLE_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (userRoleMultipleFiltersRequest.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ROLE_MULTIPLE_FILTERS_PAGE_LESS_THAN_ZERO.getCode(), new String[]{}, locale));
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
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ROLE_STRING_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
