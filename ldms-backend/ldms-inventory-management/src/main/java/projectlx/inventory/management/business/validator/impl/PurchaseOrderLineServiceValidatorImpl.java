package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.PurchaseOrderLineServiceValidator;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderLineRequest;
import projectlx.inventory.management.utils.requests.PurchaseOrderLineMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class PurchaseOrderLineServiceValidatorImpl implements PurchaseOrderLineServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderLineServiceValidatorImpl.class);

    private final ProductRepository productRepository;
    private final MessageService messageService;

    @Override
    public ValidatorDto isCreatePurchaseOrderLineRequestValid(CreatePurchaseOrderLineRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreatePurchaseOrderLineRequest is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_INVALID_REQUEST.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        if (request.getPurchaseOrderId() == null) {
            logger.info("Validation failed: Purchase Order ID is required");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_INVALID_REQUEST.getCode(),
                    new String[]{"Purchase Order ID is required"}, locale));
        }

        if (request.getProductId() == null) {
            logger.info("Validation failed: Product ID is required");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_PRODUCT_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
        } else {
            Optional<Product> productOpt = productRepository.findByIdAndEntityStatusNot(
                    request.getProductId(), EntityStatus.DELETED);

            if (productOpt.isEmpty()) {
                logger.info("Validation failed: Product not found or deleted");

                errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_PRODUCT_NOT_FOUND_OR_DELETED.getCode(),
                        new String[]{}, locale));
            }
        }

        if (request.getUnitOfMeasure() == null) {
            logger.info("Validation failed: Unit of measure is required");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_UNIT_OF_MEASURE_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (request.getQuantity() == null) {
            logger.info("Validation failed: Quantity is required");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_QUANTITY_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (request.getUnitPrice() == null) {
            logger.info("Validation failed: Unit price is required");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_UNIT_PRICE_REQUIRED.getCode(),
                    new String[]{}, locale));
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

        if (id == null || id <= 0) {
            logger.info("Validation failed: Invalid id supplied");

            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditPurchaseOrderLineRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditPurchaseOrderLineRequest is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PURCHASE_ORDER_LINE_INVALID_REQUEST.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        if (request.getPurchaseOrderLineId() == null || request.getPurchaseOrderLineId() <= 0) {
            logger.info("Validation failed: Purchase order line ID is required");

            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(),
                    new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrievePurchaseOrderLineByMultipleFilters(
            PurchaseOrderLineMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: PurchaseOrderLineMultipleFiltersRequest is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_LINE_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}
