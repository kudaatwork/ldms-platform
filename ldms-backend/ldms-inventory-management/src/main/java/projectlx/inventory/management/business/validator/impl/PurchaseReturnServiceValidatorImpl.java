package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.PurchaseReturnServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreatePurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseReturnRequest;
import projectlx.inventory.management.utils.requests.PurchaseReturnMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class PurchaseReturnServiceValidatorImpl implements PurchaseReturnServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseReturnServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreatePurchaseReturnRequestValid(CreatePurchaseReturnRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreatePurchaseReturnRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPurchaseOrderId() == null || request.getPurchaseOrderId() <= 0) {
            logger.info("Validation failed: Purchase order ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_PURCHASE_ORDER_ID_INVALID.getCode(),
                    locale));
        }

        if (request.getWarehouseLocationId() == null || request.getWarehouseLocationId() <= 0) {
            logger.info("Validation failed: Warehouse location ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_WAREHOUSE_LOCATION_ID_INVALID.getCode(),
                    locale));
        }

        if (request.getReturnedByUserId() == null || request.getReturnedByUserId() <= 0) {
            logger.info("Validation failed: Returned by user ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_RETURNED_BY_USER_ID_INVALID.getCode(),
                    locale));
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            logger.info("Validation failed: Return reason is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_REASON_MISSING.getCode(), locale));
        }

        if (request.getReturnedLineItems() == null || request.getReturnedLineItems().isEmpty()) {
            logger.info("Validation failed: Returned line items are missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_LINE_ITEMS_MISSING.getCode(), locale));
        } else {
            // Validate each returned line item
            for (int i = 0; i < request.getReturnedLineItems().size(); i++) {
                CreatePurchaseReturnRequest.ReturnedLineItem lineItem = request.getReturnedLineItems().get(i);
                
                if (lineItem.getProductId() == null || lineItem.getProductId() <= 0) {
                    logger.info("Validation failed: Line item product ID is invalid");
                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_LINE_PRODUCT_ID_INVALID.getCode(), locale));
                }
                
                if (lineItem.getQuantityReturned() == null || lineItem.getQuantityReturned().compareTo(BigDecimal.ZERO) <= 0) {
                    logger.info("Validation failed: Line item quantity returned is invalid");
                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_LINE_QUANTITY_INVALID.getCode(), locale));
                }
                
                if (lineItem.getUnitCost() == null || lineItem.getUnitCost().compareTo(BigDecimal.ZERO) <= 0) {
                    logger.info("Validation failed: Line item unit cost is invalid");
                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_RETURN_LINE_UNIT_COST_INVALID.getCode(), locale));
                }
            }
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
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
    public ValidatorDto isRequestValidForEditing(EditPurchaseReturnRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditPurchaseReturnRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_PURCHASE_RETURN_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPurchaseReturnId() == null || request.getPurchaseReturnId() <= 0) {
            logger.info("Validation failed: Purchase return ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_PURCHASE_RETURN_ID_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRequestValidToRetrievePurchaseReturnByMultipleFilters(PurchaseReturnMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: PurchaseReturnMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        // Validate pagination parameters
        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is negative");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_PAGE_NEGATIVE.getCode(), locale));
        }

        if (request.getSize() <= 0 || request.getSize() > 100) {
            logger.info("Validation failed: Page size is invalid (must be between 1 and 100)");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_RETURN_SIZE_INVALID.getCode(), locale));
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
}
