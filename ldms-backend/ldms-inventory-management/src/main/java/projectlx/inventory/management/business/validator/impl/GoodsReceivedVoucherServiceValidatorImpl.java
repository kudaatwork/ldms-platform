package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.validator.api.GoodsReceivedVoucherServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class GoodsReceivedVoucherServiceValidatorImpl implements GoodsReceivedVoucherServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(GoodsReceivedVoucherServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isReceiveGoodsRequestValid(ReceiveGoodsRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {

            logger.info("Validation failed: ReceiveGoodsRequest is null");

            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_RECEIVE_GOODS_REQUEST_IS_NULL_OR_MISSING_REQUIRED_FIELDS.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        if (request.getPurchaseOrderId() == null || request.getPurchaseOrderId() <= 0) {

            logger.info("Validation failed: purchaseOrderId is null or invalid");

            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(),
                    new String[]{}, locale));
        }

        if (request.getWarehouseLocationId() == null || request.getWarehouseLocationId() <= 0) {

            logger.info("Validation failed: warehouseLocationId is null or invalid");

            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_WAREHOUSE_LOCATION_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (request.getReceivedByUserId() == null || request.getReceivedByUserId() <= 0) {

            logger.info("Validation failed: receivedByUserId is null or invalid");

            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_USER_ID_REQUIRED.getCode(),
                    new String[]{}, locale));
        }

        if (request.getReceivedItems() == null || request.getReceivedItems().isEmpty()) {

            logger.info("Validation failed: receivedItems is null or empty");

            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_RECEIVE_GOODS_REQUEST_IS_NULL_OR_MISSING_REQUIRED_FIELDS.getCode(),
                    new String[]{}, locale));

        } else {

            for (int i = 0; i < request.getReceivedItems().size(); i++) {

                ReceiveGoodsRequest.ReceivedLineItem item = request.getReceivedItems().get(i);
                String linePrefix = "Item " + (i + 1) + ": ";

                if (item.getPurchaseOrderLineId() == null || item.getPurchaseOrderLineId() <= 0) {

                    logger.info("Validation failed: purchaseOrderLineId is null or invalid for item {}", i + 1);

                    errors.add(linePrefix + messageService.getMessage(
                            I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(),
                            new String[]{}, locale));
                }

                if (item.getQuantityReceived() == null || item.getQuantityReceived().compareTo(BigDecimal.ZERO) <= 0) {

                    logger.info("Validation failed: quantityReceived is null or not positive for item {}", i + 1);

                    errors.add(linePrefix + messageService.getMessage(
                            I18Code.MESSAGE_QUANTITY_MUST_BE_POSITIVE.getCode(),
                            new String[]{}, locale));
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

            logger.info("Validation failed: ID is null or invalid");

            errors.add(messageService.getMessage(
                    I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }
}
