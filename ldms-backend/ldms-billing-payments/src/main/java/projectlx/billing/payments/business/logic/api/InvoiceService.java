package projectlx.billing.payments.business.logic.api;

import projectlx.billing.payments.utils.responses.InvoiceResponse;

import java.util.Locale;
import java.util.Map;

public interface InvoiceService {

    InvoiceResponse list(Locale locale, String username);

    InvoiceResponse generateFromGrvEvent(Map<String, Object> event, Locale locale);

    InvoiceResponse generateFromPurchaseOrderEvent(Map<String, Object> event, Locale locale);
}
