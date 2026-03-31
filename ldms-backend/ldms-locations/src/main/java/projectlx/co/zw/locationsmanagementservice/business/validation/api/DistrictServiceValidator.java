package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditDistrictRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface DistrictServiceValidator {
    ValidatorDto isCreateDistrictRequestValid(CreateDistrictRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditDistrictRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveDistrictsByMultipleFilters(projectlx.co.zw.locationsmanagementservice.utils.requests.DistrictMultipleFiltersRequest request, Locale locale);
}
