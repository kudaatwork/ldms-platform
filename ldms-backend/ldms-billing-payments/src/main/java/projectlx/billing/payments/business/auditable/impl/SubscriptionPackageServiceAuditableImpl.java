package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.SubscriptionPackageServiceAuditable;
import projectlx.billing.payments.model.SubscriptionPackage;
import projectlx.billing.payments.repository.SubscriptionPackageRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class SubscriptionPackageServiceAuditableImpl implements SubscriptionPackageServiceAuditable {

    private final SubscriptionPackageRepository subscriptionPackageRepository;

    @Override
    public SubscriptionPackage create(SubscriptionPackage entity, Locale locale, String username) {
        return subscriptionPackageRepository.save(entity);
    }

    @Override
    public SubscriptionPackage update(SubscriptionPackage entity, Locale locale, String username) {
        return subscriptionPackageRepository.save(entity);
    }

    @Override
    public SubscriptionPackage delete(SubscriptionPackage entity, Locale locale) {
        return subscriptionPackageRepository.save(entity);
    }
}
