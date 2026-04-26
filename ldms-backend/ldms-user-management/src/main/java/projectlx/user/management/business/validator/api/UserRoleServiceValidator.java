package projectlx.user.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.utils.requests.CreateUserRoleRequest;
import projectlx.user.management.utils.requests.EditUserRoleRequest;
import projectlx.user.management.utils.requests.UserRoleMultipleFiltersRequest;

import java.util.Locale;

public interface UserRoleServiceValidator {
    // Methods returning ValidatorDto with Locale parameter
    ValidatorDto isCreateUserRoleRequestValid(CreateUserRoleRequest createUserRoleRequest, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditUserRoleRequest editUserRoleRequest, Locale locale);
    ValidatorDto isRequestValidToRetrieveUserRoleByMultipleFilters(UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest, Locale locale);
    ValidatorDto isStringValid(String input, Locale locale);

    // Methods returning ValidatorDto without Locale parameter
    default ValidatorDto isCreateUserRoleRequestValidDto(CreateUserRoleRequest createUserRoleRequest) {
        return isCreateUserRoleRequestValid(createUserRoleRequest, Locale.getDefault());
    }

    default ValidatorDto isIdValidDto(Long id) {
        return isIdValid(id, Locale.getDefault());
    }

    default ValidatorDto isRequestValidForEditingDto(EditUserRoleRequest editUserRoleRequest) {
        return isRequestValidForEditing(editUserRoleRequest, Locale.getDefault());
    }

    default ValidatorDto isRequestValidToRetrieveUserRoleByMultipleFiltersDto(UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest) {
        return isRequestValidToRetrieveUserRoleByMultipleFilters(userRoleMultipleFiltersRequest, Locale.getDefault());
    }

    default ValidatorDto isStringValidDto(String input) {
        return isStringValid(input, Locale.getDefault());
    }

    // Methods returning boolean for backward compatibility with tests
    default boolean isCreateUserRoleRequestValid(CreateUserRoleRequest createUserRoleRequest) {
        return isCreateUserRoleRequestValid(createUserRoleRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isIdValid(Long id) {
        return isIdValid(id, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidForEditing(EditUserRoleRequest editUserRoleRequest) {
        return isRequestValidForEditing(editUserRoleRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidToRetrieveUserRoleByMultipleFilters(UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest) {
        return isRequestValidToRetrieveUserRoleByMultipleFilters(userRoleMultipleFiltersRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isStringValid(String input) {
        return isStringValid(input, Locale.getDefault()).getSuccess();
    }
}
