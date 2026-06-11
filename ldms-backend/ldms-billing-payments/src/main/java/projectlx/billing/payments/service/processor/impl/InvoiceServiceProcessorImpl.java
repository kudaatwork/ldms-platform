package projectlx.billing.payments.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.logic.api.InvoiceService;
import projectlx.billing.payments.service.processor.api.InvoiceServiceProcessor;
import projectlx.billing.payments.utils.responses.InvoiceResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class InvoiceServiceProcessorImpl implements InvoiceServiceProcessor {

    private final InvoiceService invoiceService;

    @Override
    public InvoiceResponse list(Locale locale, String username) {
        return invoiceService.list(locale, username);
    }
}
