package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CityMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCityRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCityRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface CityServiceValidator {

    ValidatorDto isCreateCityRequestValid(CreateCityRequest request, Locale locale);

    ValidatorDto isIdValid(Long id, Locale locale);

    ValidatorDto isRequestValidForEditing(EditCityRequest request, Locale locale);

    ValidatorDto isRequestValidToRetrieveCitiesByMultipleFilters(CityMultipleFiltersRequest request, Locale locale);
}
