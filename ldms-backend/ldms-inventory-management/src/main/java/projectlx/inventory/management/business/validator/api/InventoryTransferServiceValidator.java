package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.EditInventoryTransferRequest;
import projectlx.inventory.management.utils.requests.InventoryTransferMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface InventoryTransferServiceValidator {
    ValidatorDto isCreateInventoryTransferRequestValid(CreateInventoryTransferRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditInventoryTransferRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveInventoryTransferByMultipleFilters(InventoryTransferMultipleFiltersRequest request,
                                                                            Locale locale);
    ValidatorDto isRejectInventoryTransferRequestValid(Long transferId, Long rejectedByUserId, String rejectionReason,
                                                         Locale locale);
}
