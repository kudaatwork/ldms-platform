package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.StockTransactionHistoryServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateStockTransactionHistoryRequest;
import projectlx.inventory.management.utils.requests.EditStockTransactionHistoryRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class StockTransactionHistoryServiceValidatorImpl implements StockTransactionHistoryServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(StockTransactionHistoryServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateStockTransactionHistoryRequestValid(CreateStockTransactionHistoryRequest request, 
                                                                    Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {

            logger.info("Validation failed: CreateStockTransactionHistoryRequest is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_STOCK_TRANSACTION_HISTORY_REQUEST_IS_NULL.getCode(),
                    locale));

            return new ValidatorDto(false, null, errors);
        }

        if (request.getInventoryItemId() == null || request.getInventoryItemId() <= 0) {

            logger.info("Validation failed: Inventory item ID is null or invalid");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_STOCK_TRANSACTION_HISTORY_INVENTORY_ITEM_ID_INVALID.getCode(),
                    locale));
        }

        if (request.getWarehouseLocationId() == null || request.getWarehouseLocationId() <= 0) {

            logger.info("Validation failed: Warehouse location ID is null or invalid");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_STOCK_TRANSACTION_HISTORY_WAREHOUSE_LOCATION_ID_INVALID.getCode(),
                    locale));
        }

        if (request.getPerformedByUserId() == null || request.getPerformedByUserId() <= 0) {

            logger.info("Validation failed: Performed by user ID is null or invalid");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_STOCK_TRANSACTION_HISTORY_PERFORMED_BY_USER_ID_INVALID.getCode(),
                    locale));
        }

        if (request.getTransactionType() == null) {

            logger.info("Validation failed: Transaction type is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_STOCK_TRANSACTION_HISTORY_TRANSACTION_TYPE_MISSING.getCode(),
                    locale));
        }

        if (request.getQuantityChange() == null || request.getQuantityChange().compareTo(BigDecimal.ZERO) == 0) {

            logger.info("Validation failed: Quantity change is null or zero");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_STOCK_TRANSACTION_HISTORY_QUANTITY_CHANGE_INVALID.getCode(),
                    locale));
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {

            logger.info("Validation failed: Reason is null or empty");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_STOCK_TRANSACTION_HISTORY_REASON_MISSING.getCode(),
                    locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (id == null || id <= 0) {
            logger.info("Validation failed: ID is null or invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditStockTransactionHistoryRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditStockTransactionHistoryRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_STOCK_TRANSACTION_HISTORY_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getStockTransactionHistoryId() == null || request.getStockTransactionHistoryId() <= 0) {
            logger.info("Validation failed: Stock transaction history ID is null or invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_STOCK_TRANSACTION_HISTORY_ID_INVALID.getCode(), locale));
        }

        if (request.getUpdatedByUserId() == null || request.getUpdatedByUserId() <= 0) {
            logger.info("Validation failed: Updated by user ID is null or invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_STOCK_TRANSACTION_HISTORY_UPDATED_BY_USER_ID_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }
}
