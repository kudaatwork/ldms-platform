package projectlx.billing.payments.service.processor.api;

import projectlx.billing.payments.utils.requests.CreatePaymentRequest;
import projectlx.billing.payments.utils.requests.RecordProcurementPaymentRequest;
import projectlx.billing.payments.utils.responses.PaymentResponse;

import java.util.Locale;

public interface PaymentServiceProcessor {

    PaymentResponse create(CreatePaymentRequest request, Locale locale, String username);

    PaymentResponse recordProcurementPayment(RecordProcurementPaymentRequest request, Locale locale, String username);

    PaymentResponse listByInvoice(Long invoiceId, Locale locale, String username);

    PaymentResponse listPendingProcurementPayments(Locale locale, String username);

    PaymentResponse verifyPayment(Long paymentId, String username, Locale locale);
}
