package projectlx.co.zw.locationsmanagementservice.business.validation.api;

import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.SuburbMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface SuburbServiceValidator {
    ValidatorDto isCreateSuburbRequestValid(CreateSuburbRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditSuburbRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveSuburbsByMultipleFilters(SuburbMultipleFiltersRequest request, Locale locale);
}