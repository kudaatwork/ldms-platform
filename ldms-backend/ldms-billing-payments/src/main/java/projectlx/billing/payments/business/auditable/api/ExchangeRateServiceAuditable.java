package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.ExchangeRate;

import java.util.Locale;

public interface ExchangeRateServiceAuditable {
    ExchangeRate create(ExchangeRate entity, Locale locale, String username);
    ExchangeRate update(ExchangeRate entity, Locale locale, String username);
    ExchangeRate delete(ExchangeRate entity, Locale locale);
}
