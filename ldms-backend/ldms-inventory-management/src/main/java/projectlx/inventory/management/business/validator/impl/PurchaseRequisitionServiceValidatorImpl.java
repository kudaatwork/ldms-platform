package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import projectlx.inventory.management.business.validator.api.PurchaseRequisitionServiceValidator;
import projectlx.inventory.management.model.PurchaseRequisition;
import projectlx.inventory.management.model.PurchaseRequisitionStatus;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.model.WarehouseLocationType;
import projectlx.inventory.management.repository.ProductRepository;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.ApprovePurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CancelPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CreatePOFromPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CreatePurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.FulfillPurchaseRequisitionLineRequest;
import projectlx.inventory.management.utils.requests.PurchaseRequisitionMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.RejectPurchaseRequisitionRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class PurchaseRequisitionServiceValidatorImpl implements PurchaseRequisitionServiceValidator {

    private final MessageService messageService;
    private final ProductRepository productRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;

    @Override
    public ValidatorDto isCreatePurchaseRequisitionRequestValid(CreatePurchaseRequisitionRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getOrganizationId() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_ORGANIZATION_ID_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (request.getDepartmentId() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_DEPARTMENT_ID_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (request.getRequestedByUserId() == null && request.getCreatedByUserId() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_REQUESTED_BY_USER_ID_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (request.getPurpose() == null || request.getPurpose().trim().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_PURPOSE_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (request.getLines() == null || request.getLines().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_LINES_REQUIRED.getCode(), new String[]{}, locale));
        } else {
            for (int i = 0; i < request.getLines().size(); i++) {
                var line = request.getLines().get(i);
                String linePrefix = "Line " + (i + 1) + ": ";

                if (line.getProductId() == null) {
                    errors.add(linePrefix + messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_LINE_PRODUCT_ID_REQUIRED.getCode(), new String[]{}, locale));
                } else {
                    boolean productExists = productRepository.findById(line.getProductId())
                            .filter(p -> p.getEntityStatus() != EntityStatus.DELETED)
                            .isPresent();
                    if (!productExists) {
                        errors.add(linePrefix + messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_LINE_PRODUCT_NOT_FOUND.getCode(), new String[]{}, locale));
                    }
                }

                if (line.getRequestedQuantity() == null || line.getRequestedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(linePrefix + messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_LINE_QUANTITY_REQUIRED.getCode(), new String[]{}, locale));
                }

                if (line.getUnitOfMeasure() == null) {
                    errors.add(linePrefix + messageService.getMessage(I18Code.MESSAGE_CREATE_PURCHASE_REQUISITION_LINE_UOM_REQUIRED.getCode(), new String[]{}, locale));
                }
            }
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (id == null || id <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isEditPurchaseRequisitionRequestValid(EditPurchaseRequisitionRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_PURCHASE_REQUISITION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getId() == null || request.getId() <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_PURCHASE_REQUISITION_ID_REQUIRED.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isEditableStatus(PurchaseRequisition purchaseRequisition, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (purchaseRequisition.getStatus() != PurchaseRequisitionStatus.DRAFT) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_EDITABLE.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isSubmittable(PurchaseRequisition purchaseRequisition, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (!purchaseRequisition.canBeSubmitted()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_SUBMITTABLE.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isApprovePurchaseRequisitionRequestValid(ApprovePurchaseRequisitionRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_APPROVE_PURCHASE_REQUISITION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getId() == null || request.getId() <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_APPROVE_PURCHASE_REQUISITION_ID_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (request.getApprovedByUserId() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_APPROVE_PURCHASE_REQUISITION_APPROVED_BY_REQUIRED.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isApprovable(PurchaseRequisition purchaseRequisition, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (!purchaseRequisition.canBeApproved()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_APPROVABLE.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRejectPurchaseRequisitionRequestValid(RejectPurchaseRequisitionRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REJECT_PURCHASE_REQUISITION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getId() == null || request.getId() <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REJECT_PURCHASE_REQUISITION_ID_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (request.getRejectedByUserId() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REJECT_PURCHASE_REQUISITION_REJECTED_BY_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_REJECT_PURCHASE_REQUISITION_REASON_REQUIRED.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isRejectable(PurchaseRequisition purchaseRequisition, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (!purchaseRequisition.canBeRejected()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_REJECTABLE.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isCancelPurchaseRequisitionRequestValid(CancelPurchaseRequisitionRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CANCEL_PURCHASE_REQUISITION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getId() == null || request.getId() <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CANCEL_PURCHASE_REQUISITION_ID_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (request.getCancelledByUserId() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CANCEL_PURCHASE_REQUISITION_CANCELLED_BY_REQUIRED.getCode(), new String[]{}, locale));
        }

        if (request.getCancellationReason() == null || request.getCancellationReason().trim().isEmpty()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CANCEL_PURCHASE_REQUISITION_REASON_REQUIRED.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isCancellable(PurchaseRequisition purchaseRequisition, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (!purchaseRequisition.canBeCancelled()) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_NOT_CANCELLABLE.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isFulfillPurchaseRequisitionLineRequestValid(FulfillPurchaseRequisitionLineRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FULFILL_PURCHASE_REQUISITION_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPurchaseRequisitionId() == null || request.getPurchaseRequisitionId() <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getLineId() == null || request.getLineId() <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FULFILL_PURCHASE_REQUISITION_LINE_NOT_FOUND.getCode(), new String[]{}, locale));
        }

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_QUANTITY_MUST_BE_POSITIVE.getCode(), new String[]{}, locale));
        }

        if (request.getFulfillmentMethod() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_FULFILL_PURCHASE_REQUISITION_INVALID_REQUEST.getCode(), new String[]{}, locale));
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isCreatePOFromPRRequestValid(CreatePOFromPurchaseRequisitionRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PO_FROM_PR_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPurchaseRequisitionId() == null || request.getPurchaseRequisitionId() <= 0) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getSupplierId() == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PO_FROM_PR_SUPPLIER_REQUIRED.getCode(), new String[]{}, locale));
        }

        validateWarehouseLocation(request.getShipFromLocationId(),
                WarehouseLocationType.SUPPLIER,
                I18Code.MESSAGE_CREATE_PURCHASE_ORDER_SHIP_FROM_LOCATION_ID_REQUIRED,
                I18Code.MESSAGE_PURCHASE_ORDER_SHIP_FROM_LOCATION_INVALID_TYPE,
                errors,
                locale);

        validateWarehouseLocation(request.getShipToLocationId(),
                WarehouseLocationType.CUSTOMER,
                I18Code.MESSAGE_CREATE_PURCHASE_ORDER_SHIP_TO_LOCATION_ID_REQUIRED,
                I18Code.MESSAGE_PURCHASE_ORDER_SHIP_TO_LOCATION_INVALID_TYPE,
                errors,
                locale);

        if (request.getReceivingWarehouseId() != null) {
            validateWarehouseLocation(request.getReceivingWarehouseId(),
                    WarehouseLocationType.CUSTOMER,
                    I18Code.MESSAGE_CREATE_PURCHASE_ORDER_RECEIVING_WAREHOUSE_ID_REQUIRED,
                    I18Code.MESSAGE_PURCHASE_ORDER_RECEIVING_WAREHOUSE_INVALID_TYPE,
                    errors,
                    locale);
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
    }

    @Override
    public ValidatorDto isMultipleFiltersRequestValid(PurchaseRequisitionMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add(messageService.getMessage(I18Code.MESSAGE_PURCHASE_REQUISITION_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPage() != null && request.getPage() < 0) {
            errors.add("Page number cannot be negative");
        }

        if (request.getSize() != null && request.getSize() <= 0) {
            errors.add("Page size must be positive");
        }

        return new ValidatorDto(errors.isEmpty(), null, errors);
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
