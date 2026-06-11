package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.PurchaseOrderServiceValidator;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.model.WarehouseLocationType;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class PurchaseOrderServiceValidatorImpl implements PurchaseOrderServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceValidatorImpl.class);

    private final ProductRepository productRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final MessageService messageService;

    @Override
    public ValidatorDto isCreatePurchaseOrderRequestValid(CreatePurchaseOrderRequest createPurchaseOrderRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (createPurchaseOrderRequest == null) {

            logger.info("Validation failed: CreatePurchaseOrderRequest is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        if (createPurchaseOrderRequest.getSupplierId() == null) {

            logger.info("Validation failed: Supplier ID is required");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_SUPPLIER_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        validateWarehouseLocation(createPurchaseOrderRequest.getShipFromLocationId(),
                WarehouseLocationType.SUPPLIER,
                I18Code.MESSAGE_CREATE_PURCHASE_ORDER_SHIP_FROM_LOCATION_ID_REQUIRED,
                I18Code.MESSAGE_PURCHASE_ORDER_SHIP_FROM_LOCATION_INVALID_TYPE,
                errors,
                locale);

        validateWarehouseLocation(createPurchaseOrderRequest.getShipToLocationId(),
                WarehouseLocationType.CUSTOMER,
                I18Code.MESSAGE_CREATE_PURCHASE_ORDER_SHIP_TO_LOCATION_ID_REQUIRED,
                I18Code.MESSAGE_PURCHASE_ORDER_SHIP_TO_LOCATION_INVALID_TYPE,
                errors,
                locale);

        validateWarehouseLocation(createPurchaseOrderRequest.getReceivingWarehouseId(),
                WarehouseLocationType.CUSTOMER,
                I18Code.MESSAGE_CREATE_PURCHASE_ORDER_RECEIVING_WAREHOUSE_ID_REQUIRED,
                I18Code.MESSAGE_PURCHASE_ORDER_RECEIVING_WAREHOUSE_INVALID_TYPE,
                errors,
                locale);

        // Validate lines and products
        if (createPurchaseOrderRequest.getLines() != null) {

            for (int i = 0; i < createPurchaseOrderRequest.getLines().size(); i++) {

                CreatePurchaseOrderRequest.PurchaseOrderLineRequest purchaseOrderLineRequest =
                        createPurchaseOrderRequest.getLines().get(i);

                String lineIndex = String.valueOf(i + 1);

                if (purchaseOrderLineRequest.getProductId() == null) {

                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_PRODUCT_ID_REQUIRED.getCode(),
                            new String[]{lineIndex}, locale));
                } else {

                    Optional<Product> productOpt = productRepository.findByIdAndEntityStatusNot(
                            purchaseOrderLineRequest.getProductId(), EntityStatus.DELETED);

                    if (productOpt.isEmpty()) {

                        errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_PRODUCT_NOT_FOUND_OR_DELETED.getCode(),
                                new String[]{lineIndex}, locale));
                    }
                }

                if (purchaseOrderLineRequest.getUnitOfMeasure() == null) {

                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_UNIT_OF_MEASURE_REQUIRED.getCode(),
                            new String[]{lineIndex}, locale));
                }

                if (purchaseOrderLineRequest.getQuantity() == null) {

                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_QUANTITY_REQUIRED.getCode(),
                            new String[]{lineIndex}, locale));
                }

                if (purchaseOrderLineRequest.getUnitPrice() == null) {

                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_ORDER_LINE_UNIT_PRICE_REQUIRED.getCode(),
                            new String[]{lineIndex}, locale));
                }
            }
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
    public ValidatorDto isRequestValidForEditing(EditPurchaseOrderRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {

            logger.info("Validation failed: EditPurchaseOrderRequest is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_PURCHASE_ORDER_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        if (request.getPurchaseOrderId() == null || request.getPurchaseOrderId() <= 0) {

            logger.info("Validation failed: Purchase order ID is required");

            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_PURCHASE_ORDER_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isApprovalValid(EditPurchaseOrderRequest request, PurchaseOrder existing, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null && existing == null) {

            logger.info("Validation failed: both request and existing purchase order are null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PURCHASE_ORDER_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        Long supplierId = (request != null && request.getSupplierId() != null) ? request.getSupplierId() :
                (existing != null ? existing.getSupplierId() : null);

        if (supplierId == null) {

            errors.add(messageService.getMessage(I18Code.MESSAGE_APPROVE_PURCHASE_ORDER_SUPPLIER_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        LocalDate orderDate = (request != null && request.getOrderDate() != null) ? request.getOrderDate() :
                (existing != null ? existing.getOrderDate() : null);

        if (orderDate == null) {

            errors.add(messageService.getMessage(I18Code.MESSAGE_APPROVE_PURCHASE_ORDER_ORDER_DATE_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    private void validateWarehouseLocation(Long warehouseLocationId,
                                           WarehouseLocationType expectedType,
                                           I18Code requiredMessage,
                                           I18Code invalidTypeMessage,
                                           List<String> errors,
                                           Locale locale) {
        if (warehouseLocationId == null) {
            errors.add(messageService.getMessage(requiredMessage.getCode(), new String[]{}, locale));
            return;
        }

        WarehouseLocation location = warehouseLocationRepository.findById(warehouseLocationId)
                .filter(item -> item.getEntityStatus() != EntityStatus.DELETED)
                .orElse(null);

        if (location == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_WAREHOUSE_LOCATION_NOT_FOUND.getCode(), new String[]{}, locale));
            return;
        }

        if (location.getWarehouseType() != expectedType) {
            errors.add(messageService.getMessage(invalidTypeMessage.getCode(), new String[]{}, locale));
        }
    }
}
