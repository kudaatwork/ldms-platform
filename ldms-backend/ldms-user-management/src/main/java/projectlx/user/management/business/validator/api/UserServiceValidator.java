package projectlx.user.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.utils.requests.CreateUserRequest;
import projectlx.user.management.utils.requests.EditUserRequest;
import projectlx.user.management.utils.requests.UsersMultipleFiltersRequest;

import java.util.List;
import java.util.Locale;

public interface UserServiceValidator {
    // Methods returning ValidatorDto with Locale parameter
    ValidatorDto isCreateUserRequestValid(CreateUserRequest createUserRequest, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditUserRequest editUserRequest, Locale locale);
    ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest, Locale locale);

    // Helper methods that still return boolean
    boolean isStringValid(String firstName);
    boolean isListValid(List<String> inputList);
    boolean isPhoneNumberValid(String phoneNumber);
    boolean isNationalIdValid(String nationalIdNumber);
    boolean isValidUserName(String name);
    boolean isPasswordValid(String password);

    // Methods returning ValidatorDto without Locale parameter (for convenience)
    default ValidatorDto isCreateUserRequestValidDto(CreateUserRequest createUserRequest) {
        return isCreateUserRequestValid(createUserRequest, Locale.getDefault());
    }

    default ValidatorDto isIdValidDto(Long id) {
        return isIdValid(id, Locale.getDefault());
    }

    default ValidatorDto isRequestValidForEditingDto(EditUserRequest editUserRequest) {
        return isRequestValidForEditing(editUserRequest, Locale.getDefault());
    }

    default ValidatorDto isRequestValidToRetrieveUsersByMultipleFiltersDto(UsersMultipleFiltersRequest usersMultipleFiltersRequest) {
        return isRequestValidToRetrieveUsersByMultipleFilters(usersMultipleFiltersRequest, Locale.getDefault());
    }

    // Methods returning boolean for backward compatibility with tests
    default boolean isCreateUserRequestValid(CreateUserRequest createUserRequest) {
        return isCreateUserRequestValid(createUserRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isIdValid(Long id) {
        return isIdValid(id, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidForEditing(EditUserRequest editUserRequest) {
        return isRequestValidForEditing(editUserRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidToRetrieveUsersByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest) {
        return isRequestValidToRetrieveUsersByMultipleFilters(usersMultipleFiltersRequest, Locale.getDefault()).getSuccess();
    }
}
