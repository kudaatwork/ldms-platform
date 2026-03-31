package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CountryMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditCountryRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface CountryServiceValidator {
    ValidatorDto isCreateCountryRequestValid(CreateCountryRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditCountryRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(CountryMultipleFiltersRequest request, Locale locale);
    ValidatorDto isStringValid(String input, Locale locale);
}
