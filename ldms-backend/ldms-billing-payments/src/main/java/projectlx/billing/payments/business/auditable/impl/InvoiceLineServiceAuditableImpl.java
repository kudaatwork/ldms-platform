package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.InvoiceLineServiceAuditable;
import projectlx.billing.payments.model.InvoiceLine;
import projectlx.billing.payments.repository.InvoiceLineRepository;

import java.util.List;

@RequiredArgsConstructor
public class InvoiceLineServiceAuditableImpl implements InvoiceLineServiceAuditable {

    private final InvoiceLineRepository invoiceLineRepository;

    @Override
    public List<InvoiceLine> createAll(List<InvoiceLine> lines, String username) {
        return invoiceLineRepository.saveAll(lines);
    }
}
