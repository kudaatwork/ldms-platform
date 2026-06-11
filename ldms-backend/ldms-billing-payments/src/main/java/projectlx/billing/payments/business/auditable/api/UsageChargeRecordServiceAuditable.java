package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.UsageChargeRecord;

import java.util.Locale;

public interface UsageChargeRecordServiceAuditable {
    UsageChargeRecord create(UsageChargeRecord entity, Locale locale, String username);
    UsageChargeRecord update(UsageChargeRecord entity, Locale locale, String username);
    UsageChargeRecord delete(UsageChargeRecord entity, Locale locale);
}
