package projectlx.billing.payments.service.processor.impl;

import projectlx.billing.payments.business.logic.api.PlatformDashboardService;
import projectlx.billing.payments.service.processor.api.PlatformDashboardServiceProcessor;
import projectlx.billing.payments.utils.responses.PlatformBillingDashboardResponse;
import projectlx.billing.payments.utils.responses.PlatformRevenueReportResponse;

import java.util.Locale;

public class PlatformDashboardServiceProcessorImpl implements PlatformDashboardServiceProcessor {

    private final PlatformDashboardService platformDashboardService;

    public PlatformDashboardServiceProcessorImpl(PlatformDashboardService platformDashboardService) {
        this.platformDashboardService = platformDashboardService;
    }

    @Override
    public PlatformBillingDashboardResponse getBillingDashboard(Locale locale) {
        return platformDashboardService.getBillingDashboard(locale);
    }

    @Override
    public PlatformRevenueReportResponse getRevenueReport(Locale locale) {
        return platformDashboardService.getRevenueReport(locale);
    }
}
