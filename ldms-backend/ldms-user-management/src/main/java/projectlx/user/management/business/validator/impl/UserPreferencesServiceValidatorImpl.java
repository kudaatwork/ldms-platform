package projectlx.user.management.business.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.validator.api.UserPreferencesServiceValidator;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.utils.requests.EditUserPreferencesRequest;
import projectlx.user.management.utils.requests.UserPreferencesMultipleFiltersRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserPreferencesServiceValidatorImpl implements UserPreferencesServiceValidator {
    private static Logger logger = LoggerFactory.getLogger(UserPreferencesServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateUserPreferencesRequestValid(CreateUserPreferencesRequest createUserPreferencesRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (createUserPreferencesRequest == null) {
            logger.info("Validation failed: CreateUserPreferencesRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PREFERENCES_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (createUserPreferencesRequest.getPreferredLanguage() == null || createUserPreferencesRequest.getPreferredLanguage().isEmpty()) {
            logger.info("Validation failed: Preferred language is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PREFERENCES_PREFERRED_LANGUAGE_MISSING.getCode(), new String[]{}, locale));
        }

        if (createUserPreferencesRequest.getTimezone() == null || createUserPreferencesRequest.getTimezone().isEmpty()) {
            logger.info("Validation failed: Timezone is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PREFERENCES_TIMEZONE_MISSING.getCode(), new String[]{}, locale));
        }

        if (createUserPreferencesRequest.getUserId() == null || createUserPreferencesRequest.getUserId() < 1) {
            logger.info("Validation failed: User ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PREFERENCES_USER_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (createUserPreferencesRequest.getTimezone() != null && !createUserPreferencesRequest.getTimezone().isEmpty() && 
                !Validators.isValidTimeZone(createUserPreferencesRequest.getTimezone())) {
            logger.info("Validation failed: Timezone is not valid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PREFERENCES_TIMEZONE_INVALID.getCode(), new String[]{}, locale));
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
    public ValidatorDto isRequestValidForEditing(EditUserPreferencesRequest editUserPreferencesRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (editUserPreferencesRequest == null) {
            logger.info("Validation failed: EditUserPreferencesRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_PREFERENCES_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (editUserPreferencesRequest.getId() == null || editUserPreferencesRequest.getId() <= 0L) {
            logger.info("Validation failed: Preferences ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_PREFERENCES_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (editUserPreferencesRequest.getPreferredLanguage() == null || editUserPreferencesRequest.getPreferredLanguage().isEmpty()) {
            logger.info("Validation failed: Preferred language is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_PREFERENCES_PREFERRED_LANGUAGE_MISSING.getCode(), new String[]{}, locale));
        }

        if (editUserPreferencesRequest.getTimezone() == null || editUserPreferencesRequest.getTimezone().isEmpty()) {
            logger.info("Validation failed: Timezone is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_PREFERENCES_TIMEZONE_MISSING.getCode(), new String[]{}, locale));
        }

        if (editUserPreferencesRequest.getUserId() == null || editUserPreferencesRequest.getUserId() < 1) {
            logger.info("Validation failed: User ID is invalid for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_PREFERENCES_USER_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (editUserPreferencesRequest.getTimezone() != null && !editUserPreferencesRequest.getTimezone().isEmpty() && 
                !Validators.isValidTimeZone(editUserPreferencesRequest.getTimezone())) {
            logger.info("Validation failed: Timezone is not valid for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_PREFERENCES_TIMEZONE_INVALID.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveUserPreferencesByMultipleFilters(UserPreferencesMultipleFiltersRequest userPreferencesMultipleFiltersRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (userPreferencesMultipleFiltersRequest == null) {
            logger.info("Validation failed: UserPreferencesMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (userPreferencesMultipleFiltersRequest.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_MULTIPLE_FILTERS_PAGE_LESS_THAN_ZERO.getCode(), new String[]{}, locale));
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
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_PREFERENCES_STRING_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
