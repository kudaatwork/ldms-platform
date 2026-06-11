package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreatePurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.PurchaseReturnMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface PurchaseReturnServiceValidator {
    ValidatorDto isCreatePurchaseReturnRequestValid(CreatePurchaseReturnRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditPurchaseReturnRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrievePurchaseReturnByMultipleFilters(PurchaseReturnMultipleFiltersRequest request, Locale locale);
    ValidatorDto isStringValid(String value, Locale locale);
}
