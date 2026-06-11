package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.SalesReservationServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateSalesReservationRequest;
import projectlx.inventory.management.utils.requests.EditSalesReservationRequest;
import projectlx.inventory.management.utils.requests.SalesReservationMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class SalesReservationServiceValidatorImpl implements SalesReservationServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(SalesReservationServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateSalesReservationRequestValid(CreateSalesReservationRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateSalesReservationRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getInventoryItemId() == null || request.getInventoryItemId() <= 0) {
            logger.info("Validation failed: Inventory item ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        if (request.getQuantityReserved() == null || request.getQuantityReserved().compareTo(BigDecimal.ZERO) <= 0) {
            logger.info("Validation failed: Quantity reserved is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), locale));
        }

        if (request.getReservedForType() == null) {
            logger.info("Validation failed: Reserved for type is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), locale));
        }

        if (request.getReservedByUserId() == null || request.getReservedByUserId() <= 0) {
            logger.info("Validation failed: Reserved by user ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
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
    public ValidatorDto isRequestValidForEditing(EditSalesReservationRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditSalesReservationRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getSalesReservationId() == null || request.getSalesReservationId() <= 0) {
            logger.info("Validation failed: Sales reservation ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveSalesReservationByMultipleFilters(SalesReservationMultipleFiltersRequest request, Locale locale) {
        return null;
    }
}
