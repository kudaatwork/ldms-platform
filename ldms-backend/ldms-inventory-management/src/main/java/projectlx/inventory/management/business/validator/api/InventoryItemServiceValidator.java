package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateInitialStockRequest;
import projectlx.inventory.management.utils.requests.CreateInventoryItemRequest;
import projectlx.inventory.management.utils.requests.EditInventoryItemRequest;
import projectlx.inventory.management.utils.requests.InventoryItemMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface InventoryItemServiceValidator {
    ValidatorDto isCreateInventoryItemRequestValid(CreateInventoryItemRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditInventoryItemRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveInventoryItemByMultipleFilters(InventoryItemMultipleFiltersRequest request, Locale locale);
    ValidatorDto isStringValid(String value, Locale locale);
    ValidatorDto isCreateInitialStockRequestValid(CreateInitialStockRequest request, Locale locale);
}
