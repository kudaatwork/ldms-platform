package projectlx.billing.payments.business.logic.api;

import projectlx.billing.payments.utils.requests.ApproveDriverExpenseRequest;
import projectlx.billing.payments.utils.requests.RejectDriverExpenseRequest;
import projectlx.billing.payments.utils.responses.DriverExpenseResponse;

import java.util.Locale;

public interface DriverExpenseReconciliationService {

    DriverExpenseResponse list(Locale locale, String username);

    DriverExpenseResponse approve(ApproveDriverExpenseRequest request, Locale locale, String username);

    DriverExpenseResponse reject(RejectDriverExpenseRequest request, Locale locale, String username);
}
