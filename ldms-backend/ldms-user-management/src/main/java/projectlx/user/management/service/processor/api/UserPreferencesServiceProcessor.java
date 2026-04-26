package projectlx.user.management.service.processor.api;

import projectlx.user.management.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.utils.requests.EditUserPreferencesRequest;
import projectlx.user.management.utils.requests.UserPreferencesMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserPreferencesResponse;
import java.util.Locale;

public interface UserPreferencesServiceProcessor {
    UserPreferencesResponse create(CreateUserPreferencesRequest createUserPreferencesRequest, Locale locale,
                                   String username);
    UserPreferencesResponse findById(Long id, Locale locale, String username);
    UserPreferencesResponse findAllAsList(String username, Locale locale);
    UserPreferencesResponse update(EditUserPreferencesRequest editUserPreferencesRequest, String username, Locale locale);
    UserPreferencesResponse delete(Long id, Locale locale, String username);
    UserPreferencesResponse findByMultipleFilters(UserPreferencesMultipleFiltersRequest userPreferencesMultipleFiltersRequest,
                                                  String username, Locale locale);
}
