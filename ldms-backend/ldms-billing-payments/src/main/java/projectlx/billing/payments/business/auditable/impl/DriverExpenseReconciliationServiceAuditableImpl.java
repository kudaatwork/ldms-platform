package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.DriverExpenseReconciliationServiceAuditable;
import projectlx.billing.payments.model.DriverExpenseReconciliation;
import projectlx.billing.payments.repository.DriverExpenseReconciliationRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class DriverExpenseReconciliationServiceAuditableImpl implements DriverExpenseReconciliationServiceAuditable {

    private final DriverExpenseReconciliationRepository driverExpenseReconciliationRepository;

    @Override
    public DriverExpenseReconciliation create(DriverExpenseReconciliation entity, Locale locale, String username) {
        return driverExpenseReconciliationRepository.save(entity);
    }

    @Override
    public DriverExpenseReconciliation update(DriverExpenseReconciliation entity, Locale locale, String username) {
        return driverExpenseReconciliationRepository.save(entity);
    }

    @Override
    public DriverExpenseReconciliation delete(DriverExpenseReconciliation entity, Locale locale) {
        return driverExpenseReconciliationRepository.save(entity);
    }
}
