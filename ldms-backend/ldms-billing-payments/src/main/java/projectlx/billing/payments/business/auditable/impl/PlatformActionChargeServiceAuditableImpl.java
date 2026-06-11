package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.PlatformActionChargeServiceAuditable;
import projectlx.billing.payments.model.PlatformActionCharge;
import projectlx.billing.payments.repository.PlatformActionChargeRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class PlatformActionChargeServiceAuditableImpl implements PlatformActionChargeServiceAuditable {

    private final PlatformActionChargeRepository platformActionChargeRepository;

    @Override
    public PlatformActionCharge create(PlatformActionCharge entity, Locale locale, String username) {
        return platformActionChargeRepository.save(entity);
    }

    @Override
    public PlatformActionCharge update(PlatformActionCharge entity, Locale locale, String username) {
        return platformActionChargeRepository.save(entity);
    }

    @Override
    public PlatformActionCharge delete(PlatformActionCharge entity, Locale locale) {
        return platformActionChargeRepository.save(entity);
    }
}
