package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.InventoryItemServiceValidator;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateInitialStockRequest;
import projectlx.inventory.management.utils.requests.CreateInventoryItemRequest;
import projectlx.inventory.management.utils.requests.EditInventoryItemRequest;
import projectlx.inventory.management.utils.requests.InventoryItemMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@RequiredArgsConstructor
public class InventoryItemServiceValidatorImpl implements InventoryItemServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(InventoryItemServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateInventoryItemRequestValid(CreateInventoryItemRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateInventoryItemRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_ITEM_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false,null, errors);
        }

        if (request.getProductId() == null || request.getProductId() <= 0) {
            logger.info("Validation failed: Product ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_ITEM_PRODUCT_ID_INVALID.getCode(), locale));
        }

        if (request.getWarehouseLocationId() == null || request.getWarehouseLocationId() <= 0) {
            logger.info("Validation failed: Warehouse location ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_ITEM_WAREHOUSE_LOCATION_ID_INVALID.getCode(), locale));
        }

        if (request.getCurrentStock() == null || request.getCurrentStock().compareTo(BigDecimal.ZERO) < 0) {
            logger.info("Validation failed: Current stock is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_ITEM_CURRENT_STOCK_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(),null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (id == null || id <= 0) {
            logger.info("Validation failed: ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }
        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditInventoryItemRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditInventoryItemRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_ITEM_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getInventoryItemId() == null || request.getInventoryItemId() <= 0) {
            logger.info("Validation failed: Inventory item ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_ITEM_ID_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveInventoryItemByMultipleFilters(InventoryItemMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: InventoryItemMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        // Validate pagination parameters
        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is negative");
            errors.add(messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_PAGE_NEGATIVE.getCode(), locale));
        }

        if (request.getSize() <= 0 || request.getSize() > InventoryExportSupport.MAX_FILTER_PAGE_SIZE) {
            logger.info("Validation failed: Page size is invalid (must be between 1 and {})",
                    InventoryExportSupport.MAX_FILTER_PAGE_SIZE);
            errors.add(messageService.getMessage(I18Code.MESSAGE_INVENTORY_ITEM_SIZE_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isStringValid(String value, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (value == null || value.trim().isEmpty()) {
            logger.info("Validation failed: String is null or empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isCreateInitialStockRequestValid(CreateInitialStockRequest request, Locale locale) {

        List<String> errorMessages = new ArrayList<>();

        // Validate Product ID
        if (request.getProductId() == null || request.getProductId() <= 0) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_PRODUCT_ID_REQUIRED.getCode(), new String[]{}, locale
            );
            errorMessages.add(message);
        }

        // Validate Warehouse Location ID
        if (request.getWarehouseLocationId() == null || request.getWarehouseLocationId() <= 0) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_WAREHOUSE_LOCATION_ID_REQUIRED.getCode(), new String[]{}, locale
            );
            errorMessages.add(message);
        }

        // Validate Supplier ID
        if (request.getSupplierId() == null || request.getSupplierId() <= 0) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_SUPPLIER_ID_REQUIRED.getCode(), new String[]{}, locale
            );
            errorMessages.add(message);
        }

        // Validate Quantity
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_QUANTITY_MUST_BE_POSITIVE.getCode(), new String[]{}, locale
            );
            errorMessages.add(message);
        }

        // Validate Unit Cost
        if (request.getUnitCost() == null || request.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_UNIT_COST_MUST_BE_NON_NEGATIVE.getCode(), new String[]{}, locale
            );
            errorMessages.add(message);
        }

        // Validate optional fields if provided
        if (request.getMinStockLevel() != null && request.getMinStockLevel().compareTo(BigDecimal.ZERO) < 0) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_MIN_STOCK_LEVEL_MUST_BE_NON_NEGATIVE.getCode(), new String[]{}, locale
            );
            errorMessages.add(message);
        }

        if (request.getReorderQuantity() != null && request.getReorderQuantity().compareTo(BigDecimal.ZERO) < 0) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_REORDER_QUANTITY_MUST_BE_NON_NEGATIVE.getCode(), new String[]{}, locale
            );
            errorMessages.add(message);
        }

        // Validate User ID
        if (request.getCreatedByUserId() == null || request.getCreatedByUserId() <= 0) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_USER_ID_REQUIRED.getCode(),
                    new String[]{},
                    locale
            );
            errorMessages.add(message);
        }

        boolean isValid = errorMessages.isEmpty();
        return new ValidatorDto(isValid, null, errorMessages);
    }


}