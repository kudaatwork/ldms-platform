package projectlx.billing.payments.service.processor.api;

import projectlx.billing.payments.utils.requests.ApproveDriverExpenseRequest;
import projectlx.billing.payments.utils.requests.RejectDriverExpenseRequest;
import projectlx.billing.payments.utils.responses.DriverExpenseResponse;

import java.util.Locale;

public interface DriverExpenseServiceProcessor {

    DriverExpenseResponse list(Locale locale, String username);

    DriverExpenseResponse approve(ApproveDriverExpenseRequest request, Locale locale, String username);

    DriverExpenseResponse reject(RejectDriverExpenseRequest request, Locale locale, String username);
}
