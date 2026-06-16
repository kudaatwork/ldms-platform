package projectlx.shipment.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.shipment.management.business.logic.api.BorderClearanceCaseService;
import projectlx.shipment.management.service.processor.api.BorderClearanceCaseServiceProcessor;
import projectlx.shipment.management.utils.requests.AddBorderClearanceDocumentRequest;
import projectlx.shipment.management.utils.requests.BorderClearanceMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.ClearBorderCaseRequest;
import projectlx.shipment.management.utils.requests.RejectBorderCaseRequest;
import projectlx.shipment.management.utils.responses.BorderClearanceCaseResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class BorderClearanceCaseServiceProcessorImpl implements BorderClearanceCaseServiceProcessor {

    private final BorderClearanceCaseService borderClearanceCaseService;

    @Override
    public BorderClearanceCaseResponse findById(Long id, Locale locale, String username) {
        return borderClearanceCaseService.findById(id, locale, username);
    }

    @Override
    public BorderClearanceCaseResponse findByMultipleFilters(BorderClearanceMultipleFiltersRequest request,
                                                              Locale locale, String username) {
        return borderClearanceCaseService.findByMultipleFilters(request, locale, username);
    }

    @Override
    public BorderClearanceCaseResponse addDocument(AddBorderClearanceDocumentRequest request,
                                                    Locale locale, String username) {
        return borderClearanceCaseService.addDocument(request, locale, username);
    }

    @Override
    public BorderClearanceCaseResponse submit(Long id, Locale locale, String username) {
        return borderClearanceCaseService.submit(id, locale, username);
    }

    @Override
    public BorderClearanceCaseResponse clear(Long id, ClearBorderCaseRequest request,
                                              Locale locale, String username) {
        return borderClearanceCaseService.clear(id, request, locale, username);
    }

    @Override
    public BorderClearanceCaseResponse reject(Long id, RejectBorderCaseRequest request,
                                               Locale locale, String username) {
        return borderClearanceCaseService.reject(id, request, locale, username);
    }
}
