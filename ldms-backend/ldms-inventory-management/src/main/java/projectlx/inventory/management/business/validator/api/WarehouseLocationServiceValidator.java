package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.EditWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.WarehouseLocationMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface WarehouseLocationServiceValidator {
    ValidatorDto isCreateWarehouseLocationRequestValid(CreateWarehouseLocationRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditWarehouseLocationRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveWarehouseLocationByMultipleFilters(WarehouseLocationMultipleFiltersRequest request, Locale locale);
    ValidatorDto isStringValid(String value, Locale locale);
}
