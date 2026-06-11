package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateStockTransactionHistoryRequest;
import projectlx.inventory.management.utils.requests.EditStockTransactionHistoryRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface StockTransactionHistoryServiceValidator {
    ValidatorDto isCreateStockTransactionHistoryRequestValid(CreateStockTransactionHistoryRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditStockTransactionHistoryRequest request, Locale locale);
}
