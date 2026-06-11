package projectlx.billing.payments.service.processor.api;

import projectlx.billing.payments.utils.responses.InvoiceResponse;

import java.util.Locale;

public interface InvoiceServiceProcessor {

    InvoiceResponse list(Locale locale, String username);
}
