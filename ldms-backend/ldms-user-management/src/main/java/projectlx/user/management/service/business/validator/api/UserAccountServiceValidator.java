package projectlx.user.management.service.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.service.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.service.utils.requests.EditUserAccountRequest;
import projectlx.user.management.service.utils.requests.UserAccountMultipleFiltersRequest;
import java.util.List;
import java.util.Locale;

public interface UserAccountServiceValidator {
    // Methods returning ValidatorDto with Locale parameter
    ValidatorDto isCreateUserRequestValid(CreateUserAccountRequest createUserAccountRequest, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditUserAccountRequest editUserAccountRequest, Locale locale);
    ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(UserAccountMultipleFiltersRequest userAccountMultipleFiltersRequest, Locale locale);
    ValidatorDto isStringValid(String input, Locale locale);
    ValidatorDto isListValid(List<String> inputList, Locale locale);
    ValidatorDto isPhoneNumberValid(String phoneNumber, Locale locale);
    ValidatorDto isNationalIdValid(String nationalIdNumber, Locale locale);
    ValidatorDto isValidUserName(String name, Locale locale);
    ValidatorDto isPasswordValid(String password, Locale locale);
    ValidatorDto isBooleanValid(Boolean isAccountLocked, Locale locale);

    // Methods returning ValidatorDto without Locale parameter
    default ValidatorDto isCreateUserRequestValidDto(CreateUserAccountRequest createUserAccountRequest) {
        return isCreateUserRequestValid(createUserAccountRequest, Locale.getDefault());
    }

    default ValidatorDto isIdValidDto(Long id) {
        return isIdValid(id, Locale.getDefault());
    }

    default ValidatorDto isRequestValidForEditingDto(EditUserAccountRequest editUserAccountRequest) {
        return isRequestValidForEditing(editUserAccountRequest, Locale.getDefault());
    }

    default ValidatorDto isRequestValidToRetrieveUsersByMultipleFiltersDto(UserAccountMultipleFiltersRequest userAccountMultipleFiltersRequest) {
        return isRequestValidToRetrieveUsersByMultipleFilters(userAccountMultipleFiltersRequest, Locale.getDefault());
    }

    default ValidatorDto isStringValidDto(String input) {
        return isStringValid(input, Locale.getDefault());
    }

    default ValidatorDto isListValidDto(List<String> inputList) {
        return isListValid(inputList, Locale.getDefault());
    }

    default ValidatorDto isPhoneNumberValidDto(String phoneNumber) {
        return isPhoneNumberValid(phoneNumber, Locale.getDefault());
    }

    default ValidatorDto isNationalIdValidDto(String nationalIdNumber) {
        return isNationalIdValid(nationalIdNumber, Locale.getDefault());
    }

    default ValidatorDto isValidUserNameDto(String name) {
        return isValidUserName(name, Locale.getDefault());
    }

    default ValidatorDto isPasswordValidDto(String password) {
        return isPasswordValid(password, Locale.getDefault());
    }

    default ValidatorDto isBooleanValidDto(Boolean isAccountLocked) {
        return isBooleanValid(isAccountLocked, Locale.getDefault());
    }

    // Methods returning boolean for backward compatibility with tests
    default boolean isCreateUserRequestValid(CreateUserAccountRequest createUserAccountRequest) {
        return isCreateUserRequestValid(createUserAccountRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isIdValid(Long id) {
        return isIdValid(id, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidForEditing(EditUserAccountRequest editUserAccountRequest) {
        return isRequestValidForEditing(editUserAccountRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidToRetrieveUsersByMultipleFilters(UserAccountMultipleFiltersRequest userAccountMultipleFiltersRequest) {
        return isRequestValidToRetrieveUsersByMultipleFilters(userAccountMultipleFiltersRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isStringValid(String input) {
        return isStringValid(input, Locale.getDefault()).getSuccess();
    }

    default boolean isListValid(List<String> inputList) {
        return isListValid(inputList, Locale.getDefault()).getSuccess();
    }

    default boolean isPhoneNumberValid(String phoneNumber) {
        return isPhoneNumberValid(phoneNumber, Locale.getDefault()).getSuccess();
    }

    default boolean isNationalIdValid(String nationalIdNumber) {
        return isNationalIdValid(nationalIdNumber, Locale.getDefault()).getSuccess();
    }

    default boolean isValidUserName(String name) {
        return isValidUserName(name, Locale.getDefault()).getSuccess();
    }

    default boolean isPasswordValid(String password) {
        return isPasswordValid(password, Locale.getDefault()).getSuccess();
    }

    default boolean isBooleanValid(Boolean isAccountLocked) {
        return isBooleanValid(isAccountLocked, Locale.getDefault()).getSuccess();
    }
}
