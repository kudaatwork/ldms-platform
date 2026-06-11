package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.InventoryTransferServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.EditInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.InventoryTransferMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class InventoryTransferServiceValidatorImpl implements InventoryTransferServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(InventoryTransferServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateInventoryTransferRequestValid(CreateInventoryTransferRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateInventoryTransferRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getTransferNumber() == null || request.getTransferNumber().trim().isEmpty()) {
            logger.info("Validation failed: Transfer number is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_NUMBER_INVALID.getCode(), locale));
        }

        if (request.getProductId() == null || request.getProductId() <= 0) {
            logger.info("Validation failed: Product ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_PRODUCT_ID_INVALID.getCode(), locale));
        }

        if (request.getFromLocationId() == null || request.getFromLocationId() <= 0) {
            logger.info("Validation failed: From location ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_FROM_LOCATION_ID_INVALID.getCode(), locale));
        }

        if (request.getToLocationId() == null || request.getToLocationId() <= 0) {
            logger.info("Validation failed: To location ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_TO_LOCATION_ID_INVALID.getCode(), locale));
        }

        if (request.getFromLocationId() != null && request.getToLocationId() != null &&
                request.getFromLocationId().equals(request.getToLocationId())) {
            logger.info("Validation failed: From and to locations are the same");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_SAME_LOCATIONS.getCode(), locale));
        }

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            logger.info("Validation failed: Quantity is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_QUANTITY_INVALID.getCode(), locale));
        }

        if (request.getUnitCost() != null && request.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            logger.info("Validation failed: Unit cost is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_UNIT_COST_INVALID.getCode(), locale));
        }

        if (request.getCreatedByUserId() == null || request.getCreatedByUserId() <= 0) {
            logger.info("Validation failed: Created by user ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_INVENTORY_TRANSFER_CREATED_BY_USER_ID_INVALID.getCode(), locale));
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
    public ValidatorDto isRequestValidForEditing(EditInventoryTransferRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditInventoryTransferRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_TRANSFER_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getInventoryTransferId() == null || request.getInventoryTransferId() <= 0) {
            logger.info("Validation failed: Inventory transfer ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_TRANSFER_ID_INVALID.getCode(), locale));
        }

        if (request.getProductId() != null && request.getProductId() <= 0) {
            logger.info("Validation failed: Product ID is invalid for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_TRANSFER_PRODUCT_ID_INVALID.getCode(), locale));
        }

        if (request.getFromLocationId() != null && request.getFromLocationId() <= 0) {
            logger.info("Validation failed: From location ID is invalid for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_TRANSFER_FROM_LOCATION_ID_INVALID.getCode(), locale));
        }

        if (request.getToLocationId() != null && request.getToLocationId() <= 0) {
            logger.info("Validation failed: To location ID is invalid for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_TRANSFER_TO_LOCATION_ID_INVALID.getCode(), locale));
        }

        if (request.getFromLocationId() != null && request.getToLocationId() != null &&
                request.getFromLocationId().equals(request.getToLocationId())) {
            logger.info("Validation failed: From and to locations are the same for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_TRANSFER_SAME_LOCATIONS.getCode(), locale));
        }

        if (request.getQuantity() != null && request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            logger.info("Validation failed: Quantity is invalid for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_TRANSFER_QUANTITY_INVALID.getCode(), locale));
        }

        if (request.getUpdatedByUserId() != null && request.getUpdatedByUserId() <= 0) {
            logger.info("Validation failed: Updated by user ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_INVENTORY_TRANSFER_UPDATED_BY_USER_ID_INVALID.getCode(), locale));
        }

        // Note: Status validation is handled in the service layer to check valid transitions
        // The validator just ensures the status enum value is valid if provided

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveInventoryTransferByMultipleFilters(InventoryTransferMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: InventoryTransferMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_REQUEST_IS_NULL.getCode(), locale));
            return new ValidatorDto(false, null, errors);
        }

        // Validate pagination parameters
        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is negative");
            errors.add(messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_PAGE_NEGATIVE.getCode(), locale));
        }

        if (request.getSize() <= 0 || request.getSize() > 100) {
            logger.info("Validation failed: Page size is invalid (must be between 1 and 100)");
            errors.add(messageService.getMessage(I18Code.MESSAGE_INVENTORY_TRANSFER_SIZE_INVALID.getCode(), locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRejectInventoryTransferRequestValid(Long transferId, Long rejectedByUserId,
                                                              String rejectionReason, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (transferId == null || transferId <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), locale));
        }

        if (rejectedByUserId == null || rejectedByUserId <= 0) {
            errors.add("Rejected by user id is required");
        }

        if (rejectionReason == null || rejectionReason.trim().length() < 3) {
            errors.add("Rejection reason is required (at least 3 characters)");
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }
}