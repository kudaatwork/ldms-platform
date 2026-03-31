package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.GeoCoordinatesMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface GeoCoordinatesServiceValidator {
    ValidatorDto isCreateGeoCoordinatesRequestValid(CreateGeoCoordinatesRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditGeoCoordinatesRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveGeoCoordinatesByMultipleFilters(GeoCoordinatesMultipleFiltersRequest request, Locale locale);
}
