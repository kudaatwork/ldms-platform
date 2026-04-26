package projectlx.user.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.utils.requests.CreateUserPasswordRequest;
import projectlx.user.management.utils.requests.UserRoleMultipleFiltersRequest;

import java.util.Locale;

public interface UserPasswordServiceValidator {
    ValidatorDto isCreateUserRequestValid(CreateUserPasswordRequest createUserPasswordRequest, Locale locale);
    ValidatorDto isStringValid(String input, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(UserRoleMultipleFiltersRequest
                                                                   userRoleMultipleFiltersRequest, Locale locale);
    ValidatorDto isRequestValidForEditing(ChangeUserPasswordRequest changeUserPasswordRequest, Locale locale);
}
