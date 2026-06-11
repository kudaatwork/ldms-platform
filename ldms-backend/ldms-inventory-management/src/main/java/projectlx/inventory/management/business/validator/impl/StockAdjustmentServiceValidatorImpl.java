package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.StockAdjustmentServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.EditStockAdjustmentRequest;
import projectlx.inventory.management.utils.requests.StockAdjustmentMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class StockAdjustmentServiceValidatorImpl implements StockAdjustmentServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(StockAdjustmentServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateStockAdjustmentRequestValid(CreateStockAdjustmentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateStockAdjustmentRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_STOCK_ADJUSTMENT_INVALID_REQUEST.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, errors);
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
    public ValidatorDto isRequestValidForEditing(EditStockAdjustmentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditStockAdjustmentRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_UPDATE_INVALID.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, errors);
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveStockAdjustmentByMultipleFilters(StockAdjustmentMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: StockAdjustmentMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        // Validate pagination parameters
        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is negative");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_PAGE_OUT_OF_BOUNDS.getCode(), locale));
        }

        if (request.getSize() <= 0 || request.getSize() > 100) {
            logger.info("Validation failed: Page size is invalid (must be between 1 and 100)");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STOCK_ADJUSTMENT_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }
}
