package projectlx.inventory.management.business.logic.support;

import org.springframework.util.StringUtils;
import projectlx.inventory.management.model.SupplierQuoteSource;
import projectlx.inventory.management.utils.requests.SubmitSupplierQuoteRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class SupplierQuoteSubmissionValidator {

    private SupplierQuoteSubmissionValidator() {
    }

    public static List<String> validate(SubmitSupplierQuoteRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Quote request is required.");
            return errors;
        }
        if (request.getPurchaseRequisitionId() == null || request.getPurchaseRequisitionId() < 1) {
            errors.add("Purchase requisition id is required.");
        }
        if (request.getSupplierOrganizationId() == null || request.getSupplierOrganizationId() < 1) {
            errors.add("Supplier organisation id is required.");
        }
        if (!StringUtils.hasText(request.getCurrency())) {
            errors.add("Currency is required.");
        }
        if (request.getPaymentTerm() == null) {
            errors.add("Payment terms are required.");
        }
        if (!StringUtils.hasText(request.getDeliveryTerms())) {
            errors.add("Delivery terms are required.");
        }
        if (request.getValidityUntil() == null) {
            errors.add("Quote validity date is required.");
        }

        SupplierQuoteSource source = request.getQuoteSource() != null
                ? request.getQuoteSource()
                : SupplierQuoteSource.SYSTEM_GENERATED;

        if (request.getLines() == null || request.getLines().isEmpty()) {
            errors.add("At least one quoted line with quantity and unit price is required.");
        } else {
            int lineIndex = 1;
            for (var line : request.getLines()) {
                if (line.getPurchaseRequisitionLineId() == null || line.getPurchaseRequisitionLineId() < 1) {
                    errors.add("Line " + lineIndex + ": purchase requisition line id is required.");
                }
                if (line.getQuotedQuantity() == null || line.getQuotedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Line " + lineIndex + ": quoted quantity must be greater than zero.");
                }
                if (line.getUnitPrice() == null || line.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Line " + lineIndex + ": unit price must be greater than zero.");
                }
                lineIndex++;
            }
        }

        if (source == SupplierQuoteSource.EXTERNAL_UPLOAD) {
            if (request.getExternalDocumentId() == null || request.getExternalDocumentId() < 1) {
                errors.add("Uploaded quote document id is required when quote source is EXTERNAL_UPLOAD.");
            }
        } else if (request.getExternalDocumentId() != null) {
            errors.add("External document id must not be set for system-generated quotes.");
        }

        return errors;
    }
}
