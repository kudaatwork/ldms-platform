package projectlx.shipment.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.shipment.management.business.auditable.api.BorderClearanceCaseServiceAuditable;
import projectlx.shipment.management.model.BorderClearanceCase;
import projectlx.shipment.management.model.BorderClearanceDocument;
import projectlx.shipment.management.repository.BorderClearanceCaseRepository;
import projectlx.shipment.management.repository.BorderClearanceDocumentRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class BorderClearanceCaseServiceAuditableImpl implements BorderClearanceCaseServiceAuditable {

    private final BorderClearanceCaseRepository borderClearanceCaseRepository;
    private final BorderClearanceDocumentRepository borderClearanceDocumentRepository;

    @Override
    public BorderClearanceCase createCase(BorderClearanceCase borderClearanceCase, Locale locale, String username) {
        return borderClearanceCaseRepository.save(borderClearanceCase);
    }

    @Override
    public BorderClearanceCase updateCase(BorderClearanceCase borderClearanceCase, Locale locale, String username) {
        return borderClearanceCaseRepository.save(borderClearanceCase);
    }

    @Override
    public BorderClearanceDocument createDocument(BorderClearanceDocument document, Locale locale, String username) {
        return borderClearanceDocumentRepository.save(document);
    }
}
