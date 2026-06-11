package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.Invoice;

import java.util.Locale;

public interface InvoiceServiceAuditable {
    Invoice create(Invoice invoice, Locale locale, String username);
    Invoice update(Invoice invoice, Locale locale, String username);
    Invoice delete(Invoice invoice, Locale locale);
}
