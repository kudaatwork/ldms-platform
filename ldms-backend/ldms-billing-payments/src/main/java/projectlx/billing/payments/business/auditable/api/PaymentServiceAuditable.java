package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.Payment;

import java.util.Locale;

public interface PaymentServiceAuditable {
    Payment create(Payment payment, Locale locale, String username);
    Payment update(Payment payment, Locale locale, String username);
    Payment delete(Payment payment, Locale locale);
}
