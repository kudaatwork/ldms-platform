package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LanguageMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface LanguageServiceValidator {
    ValidatorDto isCreateLanguageRequestValid(CreateLanguageRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditLanguageRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(LanguageMultipleFiltersRequest request, Locale locale);
}
