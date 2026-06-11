package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.model.PurchaseRequisition;
import projectlx.inventory.management.utils.requests.ApprovePurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CancelPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CreatePOFromPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.CreatePurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseRequisitionRequest;
import projectlx.inventory.management.utils.requests.FulfillPurchaseRequisitionLineRequest;
import projectlx.inventory.management.utils.requests.PurchaseRequisitionMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.RejectPurchaseRequisitionRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface PurchaseRequisitionServiceValidator {

    ValidatorDto isCreatePurchaseRequisitionRequestValid(CreatePurchaseRequisitionRequest request, Locale locale);

    ValidatorDto isIdValid(Long id, Locale locale);

    ValidatorDto isEditPurchaseRequisitionRequestValid(EditPurchaseRequisitionRequest request, Locale locale);

    ValidatorDto isEditableStatus(PurchaseRequisition purchaseRequisition, Locale locale);

    ValidatorDto isSubmittable(PurchaseRequisition purchaseRequisition, Locale locale);

    ValidatorDto isApprovePurchaseRequisitionRequestValid(ApprovePurchaseRequisitionRequest request, Locale locale);

    ValidatorDto isApprovable(PurchaseRequisition purchaseRequisition, Locale locale);

    ValidatorDto isRejectPurchaseRequisitionRequestValid(RejectPurchaseRequisitionRequest request, Locale locale);

    ValidatorDto isRejectable(PurchaseRequisition purchaseRequisition, Locale locale);

    ValidatorDto isCancelPurchaseRequisitionRequestValid(CancelPurchaseRequisitionRequest request, Locale locale);

    ValidatorDto isCancellable(PurchaseRequisition purchaseRequisition, Locale locale);

    ValidatorDto isFulfillPurchaseRequisitionLineRequestValid(FulfillPurchaseRequisitionLineRequest request, Locale locale);

    ValidatorDto isCreatePOFromPRRequestValid(CreatePOFromPurchaseRequisitionRequest request, Locale locale);

    ValidatorDto isMultipleFiltersRequestValid(PurchaseRequisitionMultipleFiltersRequest request, Locale locale);
}
