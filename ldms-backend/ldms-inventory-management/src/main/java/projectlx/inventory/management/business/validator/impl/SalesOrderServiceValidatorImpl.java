package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.validator.api.SalesOrderServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateSalesOrderRequest;
import projectlx.inventory.management.utils.requests.EditSalesOrderRequest;
import projectlx.inventory.management.utils.requests.FulfillSalesOrderRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class SalesOrderServiceValidatorImpl implements SalesOrderServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(SalesOrderServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateSalesOrderRequestValid(CreateSalesOrderRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateSalesOrderRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_SALES_ORDER_INVALID_REQUEST.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getCustomerId() == null || request.getCustomerId() <= 0) {
            logger.info("Validation failed: Customer ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        if (request.getOrderDate() == null) {
            logger.info("Validation failed: Order date is null");
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
    public ValidatorDto isRequestValidForEditing(EditSalesOrderRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {

            logger.info("Validation failed: EditSalesOrderRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_SALES_ORDER_INVALID_REQUEST.getCode(), locale));

            return new ValidatorDto(false, null, errors);
        }

        if (request.getSalesOrderId() == null || request.getSalesOrderId() <= 0) {
            logger.info("Validation failed: Sales order ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isFulfillOrderRequestValid(FulfillSalesOrderRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {

            logger.info("Validation failed: FulfillSalesOrderRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_FULFILL_SALES_ORDER_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getSalesOrderId() == null || request.getSalesOrderId() <= 0) {

            logger.info("Validation failed: Sales order ID is invalid for fulfillment");
            errors.add(messageService.getMessage(I18Code.MESSAGE_SALES_ORDER_ID_SUPPLIED_IS_INVALID_FOR_FULLFILLMENT.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }
}
