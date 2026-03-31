package projectlx.user.management.service.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.service.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.service.utils.requests.EditUserSecurityRequest;
import projectlx.user.management.service.utils.requests.UserSecurityMultipleFiltersRequest;

import java.util.Locale;

public interface UserSecurityServiceValidator {
    // Methods returning ValidatorDto with Locale parameter
    ValidatorDto isCreateUserSecurityRequestValid(CreateUserSecurityRequest createUserSecurityRequest, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditUserSecurityRequest editUserSecurityRequest, Locale locale);
    ValidatorDto isRequestValidToRetrieveUserSecurityByMultipleFilters(UserSecurityMultipleFiltersRequest userSecurityMultipleFiltersRequest, Locale locale);
    ValidatorDto isStringValid(String input, Locale locale);
    ValidatorDto isBooleanValid(Boolean isTwoFactorEnabled, Locale locale);

    // Methods returning ValidatorDto without Locale parameter
    default ValidatorDto isCreateUserSecurityRequestValidDto(CreateUserSecurityRequest createUserSecurityRequest) {
        return isCreateUserSecurityRequestValid(createUserSecurityRequest, Locale.getDefault());
    }

    default ValidatorDto isIdValidDto(Long id) {
        return isIdValid(id, Locale.getDefault());
    }

    default ValidatorDto isRequestValidForEditingDto(EditUserSecurityRequest editUserSecurityRequest) {
        return isRequestValidForEditing(editUserSecurityRequest, Locale.getDefault());
    }

    default ValidatorDto isRequestValidToRetrieveUserSecurityByMultipleFiltersDto(UserSecurityMultipleFiltersRequest userSecurityMultipleFiltersRequest) {
        return isRequestValidToRetrieveUserSecurityByMultipleFilters(userSecurityMultipleFiltersRequest, Locale.getDefault());
    }

    default ValidatorDto isStringValidDto(String input) {
        return isStringValid(input, Locale.getDefault());
    }

    default ValidatorDto isBooleanValidDto(Boolean isTwoFactorEnabled) {
        return isBooleanValid(isTwoFactorEnabled, Locale.getDefault());
    }

    // Methods returning boolean for backward compatibility with tests
    default boolean isCreateUserSecurityRequestValid(CreateUserSecurityRequest createUserSecurityRequest) {
        return isCreateUserSecurityRequestValid(createUserSecurityRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isIdValid(Long id) {
        return isIdValid(id, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidForEditing(EditUserSecurityRequest editUserSecurityRequest) {
        return isRequestValidForEditing(editUserSecurityRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidToRetrieveUserSecurityByMultipleFilters(UserSecurityMultipleFiltersRequest userSecurityMultipleFiltersRequest) {
        return isRequestValidToRetrieveUserSecurityByMultipleFilters(userSecurityMultipleFiltersRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isStringValid(String input) {
        return isStringValid(input, Locale.getDefault()).getSuccess();
    }

    default boolean isBooleanValid(Boolean isTwoFactorEnabled) {
        return isBooleanValid(isTwoFactorEnabled, Locale.getDefault()).getSuccess();
    }
}
