package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.OrganizationBillingSettingServiceAuditable;
import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.repository.OrganizationBillingSettingRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class OrganizationBillingSettingServiceAuditableImpl implements OrganizationBillingSettingServiceAuditable {

    private final OrganizationBillingSettingRepository organizationBillingSettingRepository;

    @Override
    public OrganizationBillingSetting create(OrganizationBillingSetting entity, Locale locale, String username) {
        return organizationBillingSettingRepository.save(entity);
    }

    @Override
    public OrganizationBillingSetting update(OrganizationBillingSetting entity, Locale locale, String username) {
        return organizationBillingSettingRepository.save(entity);
    }

    @Override
    public OrganizationBillingSetting delete(OrganizationBillingSetting entity, Locale locale) {
        return organizationBillingSettingRepository.save(entity);
    }
}
