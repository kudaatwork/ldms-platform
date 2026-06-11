package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateSalesReservationRequest;
import projectlx.inventory.management.utils.requests.EditSalesReservationRequest;
import projectlx.inventory.management.utils.requests.SalesReservationMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface SalesReservationServiceValidator {
    ValidatorDto isCreateSalesReservationRequestValid(CreateSalesReservationRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditSalesReservationRequest request, Locale locale);

    ValidatorDto isRequestValidToRetrieveSalesReservationByMultipleFilters(SalesReservationMultipleFiltersRequest request, Locale locale);
}
