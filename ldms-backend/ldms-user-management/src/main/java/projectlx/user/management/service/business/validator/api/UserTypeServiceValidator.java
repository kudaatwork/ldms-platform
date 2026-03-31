package projectlx.user.management.service.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.service.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.service.utils.requests.EditUserTypeRequest;
import projectlx.user.management.service.utils.requests.UserTypeMultipleFiltersRequest;

import java.util.Locale;

public interface UserTypeServiceValidator {
    // Methods returning ValidatorDto with Locale parameter
    ValidatorDto isCreateUserTypeRequestValid(CreateUserTypeRequest createUserTypeRequest, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditUserTypeRequest editUserTypeRequest, Locale locale);
    ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest, Locale locale);
    ValidatorDto isStringValid(String input, Locale locale);

    // Methods returning ValidatorDto without Locale parameter
    default ValidatorDto isCreateUserTypeRequestValidDto(CreateUserTypeRequest createUserTypeRequest) {
        return isCreateUserTypeRequestValid(createUserTypeRequest, Locale.getDefault());
    }

    default ValidatorDto isIdValidDto(Long id) {
        return isIdValid(id, Locale.getDefault());
    }

    default ValidatorDto isRequestValidForEditingDto(EditUserTypeRequest editUserTypeRequest) {
        return isRequestValidForEditing(editUserTypeRequest, Locale.getDefault());
    }

    default ValidatorDto isRequestValidToRetrieveUsersByMultipleFiltersDto(UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest) {
        return isRequestValidToRetrieveUsersByMultipleFilters(userTypeMultipleFiltersRequest, Locale.getDefault());
    }

    default ValidatorDto isStringValidDto(String input) {
        return isStringValid(input, Locale.getDefault());
    }

    // Methods returning boolean for backward compatibility with tests
    default boolean isCreateUserTypeRequestValid(CreateUserTypeRequest createUserTypeRequest) {
        return isCreateUserTypeRequestValid(createUserTypeRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isIdValid(Long id) {
        return isIdValid(id, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidForEditing(EditUserTypeRequest editUserTypeRequest) {
        return isRequestValidForEditing(editUserTypeRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidToRetrieveUsersByMultipleFilters(UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest) {
        return isRequestValidToRetrieveUsersByMultipleFilters(userTypeMultipleFiltersRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isStringValid(String input) {
        return isStringValid(input, Locale.getDefault()).getSuccess();
    }
}
