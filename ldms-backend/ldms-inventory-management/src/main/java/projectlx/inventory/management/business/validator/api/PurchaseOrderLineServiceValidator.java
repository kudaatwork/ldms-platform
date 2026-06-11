package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreatePurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.PurchaseOrderLineMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface PurchaseOrderLineServiceValidator {
    ValidatorDto isCreatePurchaseOrderLineRequestValid(CreatePurchaseOrderLineRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditPurchaseOrderLineRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrievePurchaseOrderLineByMultipleFilters(PurchaseOrderLineMultipleFiltersRequest request, Locale locale);
}
