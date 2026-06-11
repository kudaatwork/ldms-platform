package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.validator.api.WarehouseLocationServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.EditWarehouseLocationRequest;
import projectlx.inventory.management.utils.requests.WarehouseLocationMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrEmpty;

@RequiredArgsConstructor
public class WarehouseLocationServiceValidatorImpl implements WarehouseLocationServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseLocationServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateWarehouseLocationRequestValid(CreateWarehouseLocationRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateWarehouseLocationRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_WAREHOUSE_LOCATION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrEmpty(request.getLine1())) {
            logger.info("Validation failed: Address line1 is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_WAREHOUSE_LOCATION_LINE1_MISSING.getCode(), new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getName())) {
            logger.info("Validation failed: Warehouse location name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_WAREHOUSE_LOCATION_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getDescription())) {
            logger.info("Validation failed: Warehouse location description is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_WAREHOUSE_LOCATION_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (id == null || id <= 0L) {
            logger.info("Validation failed: ID is null or invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditWarehouseLocationRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditWarehouseLocationRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_WAREHOUSE_LOCATION_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getWarehouseLocationId() == null || request.getWarehouseLocationId() <= 0L) {
            logger.info("Validation failed: Warehouse location ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_WAREHOUSE_LOCATION_ID_INVALID.getCode(),
                    new String[]{}, locale));
        }

        // Validate optional fields if provided: they should not be empty strings
        if (request.getName() != null && request.getName().trim().isEmpty()) {
            logger.info("Validation failed: Warehouse location name is empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_WAREHOUSE_LOCATION_NAME_MISSING.getCode(), new String[]{}, locale));
        }
        if (request.getDescription() != null && request.getDescription().trim().isEmpty()) {
            logger.info("Validation failed: Warehouse location description is empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_WAREHOUSE_LOCATION_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveWarehouseLocationByMultipleFilters(WarehouseLocationMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: WarehouseLocationMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is negative");
            errors.add(messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_PAGE_NEGATIVE.getCode(), new String[]{}, locale));
        }

        if (request.getSize() <= 0 || request.getSize() > 100) {
            logger.info("Validation failed: Page size is invalid (must be between 1 and 100)");
            errors.add(messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_SIZE_INVALID.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isStringValid(String value, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (isNullOrEmpty(value)) {
            logger.info("Validation failed: String is null or empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
