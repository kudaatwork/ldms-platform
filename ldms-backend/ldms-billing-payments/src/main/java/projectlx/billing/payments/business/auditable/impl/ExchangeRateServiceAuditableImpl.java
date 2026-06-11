package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.ExchangeRateServiceAuditable;
import projectlx.billing.payments.model.ExchangeRate;
import projectlx.billing.payments.repository.ExchangeRateRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class ExchangeRateServiceAuditableImpl implements ExchangeRateServiceAuditable {

    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public ExchangeRate create(ExchangeRate entity, Locale locale, String username) {
        return exchangeRateRepository.save(entity);
    }

    @Override
    public ExchangeRate update(ExchangeRate entity, Locale locale, String username) {
        return exchangeRateRepository.save(entity);
    }

    @Override
    public ExchangeRate delete(ExchangeRate entity, Locale locale) {
        return exchangeRateRepository.save(entity);
    }
}
