package projectlx.billing.payments.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.logic.api.DriverExpenseReconciliationService;
import projectlx.billing.payments.service.processor.api.DriverExpenseServiceProcessor;
import projectlx.billing.payments.utils.requests.ApproveDriverExpenseRequest;
import projectlx.billing.payments.utils.requests.RejectDriverExpenseRequest;
import projectlx.billing.payments.utils.responses.DriverExpenseResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class DriverExpenseServiceProcessorImpl implements DriverExpenseServiceProcessor {

    private final DriverExpenseReconciliationService driverExpenseReconciliationService;

    @Override
    public DriverExpenseResponse list(Locale locale, String username) {
        return driverExpenseReconciliationService.list(locale, username);
    }

    @Override
    public DriverExpenseResponse approve(ApproveDriverExpenseRequest request, Locale locale, String username) {
        return driverExpenseReconciliationService.approve(request, locale, username);
    }

    @Override
    public DriverExpenseResponse reject(RejectDriverExpenseRequest request, Locale locale, String username) {
        return driverExpenseReconciliationService.reject(request, locale, username);
    }
}
