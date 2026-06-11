package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.SalesOrderLineServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderLineRequest;
import projectlx.inventory.management.utils.requests.SalesOrderLineMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class SalesOrderLineServiceValidatorImpl implements SalesOrderLineServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(SalesOrderLineServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateSalesOrderLineRequestValid(CreateSalesOrderLineRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateSalesOrderLineRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_SALES_ORDER_LINE_INVALID_REQUEST.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getSalesOrderId() == null || request.getSalesOrderId() <= 0) {
            logger.info("Validation failed: Sales order ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        if (request.getProductId() == null || request.getProductId() <= 0) {
            logger.info("Validation failed: Product ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            logger.info("Validation failed: Quantity is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), locale));
        }

        if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            logger.info("Validation failed: Unit price is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), locale));
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
    public ValidatorDto isRequestValidForEditing(EditSalesOrderLineRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditSalesOrderLineRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_SALES_ORDER_LINE_INVALID_REQUEST.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getSalesOrderLineId() == null || request.getSalesOrderLineId() <= 0) {
            logger.info("Validation failed: Sales order line ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveSalesOrderLineByMultipleFilters(SalesOrderLineMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: SalesOrderLineMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        // Validate pagination parameters
        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is negative");
            errors.add(messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_PAGE_OUT_OF_BOUNDS.getCode(), locale));
        }

        if (request.getSize() <= 0 || request.getSize() > 100) {
            logger.info("Validation failed: Page size is invalid (must be between 1 and 100)");
            errors.add(messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_LINE_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }
}
