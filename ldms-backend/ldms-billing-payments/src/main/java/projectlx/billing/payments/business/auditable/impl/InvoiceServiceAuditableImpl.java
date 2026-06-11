package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.InvoiceServiceAuditable;
import projectlx.billing.payments.model.Invoice;
import projectlx.billing.payments.repository.InvoiceRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class InvoiceServiceAuditableImpl implements InvoiceServiceAuditable {

    private final InvoiceRepository invoiceRepository;

    @Override
    public Invoice create(Invoice invoice, Locale locale, String username) {
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice update(Invoice invoice, Locale locale, String username) {
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice delete(Invoice invoice, Locale locale) {
        return invoiceRepository.save(invoice);
    }
}
