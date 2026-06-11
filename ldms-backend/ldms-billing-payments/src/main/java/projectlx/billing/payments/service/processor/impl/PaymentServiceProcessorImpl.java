package projectlx.billing.payments.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import projectlx.billing.payments.business.logic.api.PaymentService;
import projectlx.billing.payments.service.processor.api.PaymentServiceProcessor;
import projectlx.billing.payments.utils.requests.CreatePaymentRequest;
import projectlx.billing.payments.utils.requests.RecordProcurementPaymentRequest;
import projectlx.billing.payments.utils.responses.PaymentResponse;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PaymentServiceProcessorImpl implements PaymentServiceProcessor {

    private final PaymentService paymentService;

    @Override
    public PaymentResponse create(CreatePaymentRequest request, Locale locale, String username) {
        return paymentService.create(request, locale, username);
    }

    @Override
    public PaymentResponse recordProcurementPayment(RecordProcurementPaymentRequest request,
                                                    Locale locale,
                                                    String username) {
        return paymentService.recordProcurementPayment(request, locale, username);
    }

    @Override
    public PaymentResponse listByInvoice(Long invoiceId, Locale locale, String username) {
        return paymentService.listByInvoice(invoiceId, locale, username);
    }

    @Override
    public PaymentResponse verifyPayment(Long paymentId, String username, Locale locale) {
        return paymentService.verifyPayment(paymentId, username, locale);
    }
}
