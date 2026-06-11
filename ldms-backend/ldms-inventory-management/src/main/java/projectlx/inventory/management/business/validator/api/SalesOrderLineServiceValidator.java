package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.SalesOrderLineMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface SalesOrderLineServiceValidator {
    ValidatorDto isCreateSalesOrderLineRequestValid(CreateSalesOrderLineRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditSalesOrderLineRequest request, Locale locale);

    ValidatorDto isRequestValidToRetrieveSalesOrderLineByMultipleFilters(SalesOrderLineMultipleFiltersRequest request, Locale locale);
}
