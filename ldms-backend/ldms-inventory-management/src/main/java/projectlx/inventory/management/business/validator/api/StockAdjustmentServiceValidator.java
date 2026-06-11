package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.EditStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.StockAdjustmentMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface StockAdjustmentServiceValidator {
    ValidatorDto isCreateStockAdjustmentRequestValid(CreateStockAdjustmentRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditStockAdjustmentRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveStockAdjustmentByMultipleFilters(StockAdjustmentMultipleFiltersRequest request, Locale locale);
}
