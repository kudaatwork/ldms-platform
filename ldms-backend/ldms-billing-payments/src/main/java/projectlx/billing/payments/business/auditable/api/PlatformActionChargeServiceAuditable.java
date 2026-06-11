package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.PlatformActionCharge;

import java.util.Locale;

public interface PlatformActionChargeServiceAuditable {
    PlatformActionCharge create(PlatformActionCharge entity, Locale locale, String username);
    PlatformActionCharge update(PlatformActionCharge entity, Locale locale, String username);
    PlatformActionCharge delete(PlatformActionCharge entity, Locale locale);
}
