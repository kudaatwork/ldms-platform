package projectlx.inventory.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.inventory.management.utils.requests.CreateDepartmentRequest;
import projectlx.inventory.management.utils.requests.DepartmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.EditDepartmentRequest;

import java.util.Locale;

public interface DepartmentServiceValidator {

    ValidatorDto isCreateDepartmentRequestValid(CreateDepartmentRequest request, Locale locale);

    ValidatorDto isIdValid(Long id, Locale locale);

    ValidatorDto isRequestValidForEditing(EditDepartmentRequest request, Locale locale);

    ValidatorDto isRequestValidToRetrieveDepartmentByMultipleFilters(DepartmentMultipleFiltersRequest request, Locale locale);

    ValidatorDto isStringValid(String value, Locale locale);
}
