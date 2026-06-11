package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.SubscriptionPackage;

import java.util.Locale;

public interface SubscriptionPackageServiceAuditable {
    SubscriptionPackage create(SubscriptionPackage entity, Locale locale, String username);
    SubscriptionPackage update(SubscriptionPackage entity, Locale locale, String username);
    SubscriptionPackage delete(SubscriptionPackage entity, Locale locale);
}
