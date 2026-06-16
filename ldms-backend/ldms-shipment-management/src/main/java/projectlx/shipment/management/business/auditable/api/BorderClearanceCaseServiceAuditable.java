package projectlx.shipment.management.business.auditable.api;

import projectlx.shipment.management.model.BorderClearanceCase;
import projectlx.shipment.management.model.BorderClearanceDocument;

import java.util.Locale;

public interface BorderClearanceCaseServiceAuditable {

    BorderClearanceCase createCase(BorderClearanceCase borderClearanceCase, Locale locale, String username);

    BorderClearanceCase updateCase(BorderClearanceCase borderClearanceCase, Locale locale, String username);

    BorderClearanceDocument createDocument(BorderClearanceDocument document, Locale locale, String username);
}
