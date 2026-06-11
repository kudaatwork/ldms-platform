package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.OrganizationBillingSetting;

import java.util.Locale;

public interface OrganizationBillingSettingServiceAuditable {
    OrganizationBillingSetting create(OrganizationBillingSetting entity, Locale locale, String username);
    OrganizationBillingSetting update(OrganizationBillingSetting entity, Locale locale, String username);
    OrganizationBillingSetting delete(OrganizationBillingSetting entity, Locale locale);
}
