package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocalizedNameMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface LocalizedNameServiceValidator {
    ValidatorDto isCreateLocalizedNameRequestValid(CreateLocalizedNameRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditLocalizedNameRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveLocalizedNamesByMultipleFilters(LocalizedNameMultipleFiltersRequest request,
                                                                         Locale locale);
}