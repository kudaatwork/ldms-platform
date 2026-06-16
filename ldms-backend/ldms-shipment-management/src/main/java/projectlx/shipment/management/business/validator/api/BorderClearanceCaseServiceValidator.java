package projectlx.shipment.management.business.validator.api;

import projectlx.shipment.management.utils.requests.AddBorderClearanceDocumentRequest;
import projectlx.shipment.management.utils.requests.BorderClearanceMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.ClearBorderCaseRequest;
import projectlx.shipment.management.utils.requests.RejectBorderCaseRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface BorderClearanceCaseServiceValidator {

    ValidatorDto isAddDocumentRequestValid(AddBorderClearanceDocumentRequest request, Locale locale);

    ValidatorDto isClearBorderCaseRequestValid(Long caseId, ClearBorderCaseRequest request, Locale locale);

    ValidatorDto isRejectBorderCaseRequestValid(Long caseId, RejectBorderCaseRequest request, Locale locale);

    ValidatorDto isMultipleFiltersRequestValid(BorderClearanceMultipleFiltersRequest request, Locale locale);
}
