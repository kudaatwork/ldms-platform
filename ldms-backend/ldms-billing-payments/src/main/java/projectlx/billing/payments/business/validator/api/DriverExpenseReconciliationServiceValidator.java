package projectlx.billing.payments.business.validator.api;

import projectlx.billing.payments.utils.requests.ApproveDriverExpenseRequest;
import projectlx.billing.payments.utils.requests.RejectDriverExpenseRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface DriverExpenseReconciliationServiceValidator {
    ValidatorDto isApproveDriverExpenseRequestValid(ApproveDriverExpenseRequest request, Locale locale);
    ValidatorDto isRejectDriverExpenseRequestValid(RejectDriverExpenseRequest request, Locale locale);
}
