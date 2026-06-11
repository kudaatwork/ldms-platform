package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.utils.requests.CreatePurchaseOrderRequest;
import projectlx.inventory.management.utils.requests.EditPurchaseOrderRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface PurchaseOrderServiceValidator {
    ValidatorDto isCreatePurchaseOrderRequestValid(CreatePurchaseOrderRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditPurchaseOrderRequest request, Locale locale);
    ValidatorDto isApprovalValid(EditPurchaseOrderRequest request, PurchaseOrder existing, Locale locale);
}
