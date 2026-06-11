package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.InvoiceLine;

import java.util.List;

public interface InvoiceLineServiceAuditable {
    List<InvoiceLine> createAll(List<InvoiceLine> lines, String username);
}
