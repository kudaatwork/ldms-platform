package projectlx.user.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.utils.requests.EditUserPreferencesRequest;
import projectlx.user.management.utils.requests.UserPreferencesMultipleFiltersRequest;

import java.util.Locale;

public interface UserPreferencesServiceValidator {
    ValidatorDto isCreateUserPreferencesRequestValid(CreateUserPreferencesRequest createUserPreferencesRequest, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditUserPreferencesRequest editUserPreferencesRequest, Locale locale);
    ValidatorDto isRequestValidToRetrieveUserPreferencesByMultipleFilters(UserPreferencesMultipleFiltersRequest userPreferencesMultipleFiltersRequest, Locale locale);
    ValidatorDto isStringValid(String input, Locale locale);
}
