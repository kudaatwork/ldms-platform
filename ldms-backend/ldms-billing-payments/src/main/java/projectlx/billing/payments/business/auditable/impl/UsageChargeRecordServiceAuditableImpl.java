package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.UsageChargeRecordServiceAuditable;
import projectlx.billing.payments.model.UsageChargeRecord;
import projectlx.billing.payments.repository.UsageChargeRecordRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class UsageChargeRecordServiceAuditableImpl implements UsageChargeRecordServiceAuditable {

    private final UsageChargeRecordRepository usageChargeRecordRepository;

    @Override
    public UsageChargeRecord create(UsageChargeRecord entity, Locale locale, String username) {
        return usageChargeRecordRepository.save(entity);
    }

    @Override
    public UsageChargeRecord update(UsageChargeRecord entity, Locale locale, String username) {
        return usageChargeRecordRepository.save(entity);
    }

    @Override
    public UsageChargeRecord delete(UsageChargeRecord entity, Locale locale) {
        return usageChargeRecordRepository.save(entity);
    }
}
