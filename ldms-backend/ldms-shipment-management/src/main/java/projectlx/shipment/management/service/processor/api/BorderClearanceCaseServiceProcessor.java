package projectlx.shipment.management.service.processor.api;

import projectlx.shipment.management.utils.requests.AddBorderClearanceDocumentRequest;
import projectlx.shipment.management.utils.requests.BorderClearanceMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.ClearBorderCaseRequest;
import projectlx.shipment.management.utils.requests.RejectBorderCaseRequest;
import projectlx.shipment.management.utils.responses.BorderClearanceCaseResponse;

import java.util.Locale;

public interface BorderClearanceCaseServiceProcessor {

    BorderClearanceCaseResponse findById(Long id, Locale locale, String username);

    BorderClearanceCaseResponse findByMultipleFilters(BorderClearanceMultipleFiltersRequest request, Locale locale, String username);

    BorderClearanceCaseResponse addDocument(AddBorderClearanceDocumentRequest request, Locale locale, String username);

    BorderClearanceCaseResponse submit(Long id, Locale locale, String username);

    BorderClearanceCaseResponse clear(Long id, ClearBorderCaseRequest request, Locale locale, String username);

    BorderClearanceCaseResponse reject(Long id, RejectBorderCaseRequest request, Locale locale, String username);
}
