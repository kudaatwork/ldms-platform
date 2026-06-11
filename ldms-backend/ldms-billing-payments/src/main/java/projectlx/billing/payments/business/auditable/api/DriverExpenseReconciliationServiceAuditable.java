package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.DriverExpenseReconciliation;

import java.util.Locale;

public interface DriverExpenseReconciliationServiceAuditable {
    DriverExpenseReconciliation create(DriverExpenseReconciliation entity, Locale locale, String username);
    DriverExpenseReconciliation update(DriverExpenseReconciliation entity, Locale locale, String username);
    DriverExpenseReconciliation delete(DriverExpenseReconciliation entity, Locale locale);
}
