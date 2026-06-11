package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.PaymentServiceAuditable;
import projectlx.billing.payments.model.Payment;
import projectlx.billing.payments.repository.PaymentRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class PaymentServiceAuditableImpl implements PaymentServiceAuditable {

    private final PaymentRepository paymentRepository;

    @Override
    public Payment create(Payment payment, Locale locale, String username) {
        return paymentRepository.save(payment);
    }

    @Override
    public Payment update(Payment payment, Locale locale, String username) {
        return paymentRepository.save(payment);
    }

    @Override
    public Payment delete(Payment payment, Locale locale) {
        return paymentRepository.save(payment);
    }
}
