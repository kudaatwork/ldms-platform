package projectlx.billing.payments.business.logic.impl;

import projectlx.billing.payments.business.logic.api.PlatformDashboardService;
import projectlx.billing.payments.business.logic.support.PlatformDashboardSupport;
import projectlx.billing.payments.business.logic.support.PlatformRevenueSupport;
import projectlx.billing.payments.utils.dtos.PlatformBillingDashboardDto;
import projectlx.billing.payments.utils.responses.PlatformBillingDashboardResponse;
import projectlx.billing.payments.utils.responses.PlatformRevenueReportResponse;

import java.util.Locale;

public class PlatformDashboardServiceImpl implements PlatformDashboardService {

    private final PlatformDashboardSupport platformDashboardSupport;
    private final PlatformRevenueSupport platformRevenueSupport;

    public PlatformDashboardServiceImpl(
            PlatformDashboardSupport platformDashboardSupport,
            PlatformRevenueSupport platformRevenueSupport) {
        this.platformDashboardSupport = platformDashboardSupport;
        this.platformRevenueSupport = platformRevenueSupport;
    }

    @Override
    public PlatformBillingDashboardResponse getBillingDashboard(Locale locale) {
        PlatformBillingDashboardDto dashboard = platformDashboardSupport.buildDashboardSnapshot();
        PlatformBillingDashboardResponse response = new PlatformBillingDashboardResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Platform billing dashboard loaded");
        response.setPlatformBillingDashboardDto(dashboard);
        return response;
    }

    @Override
    public PlatformRevenueReportResponse getRevenueReport(Locale locale) {
        PlatformRevenueReportResponse response = new PlatformRevenueReportResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Platform revenue report loaded");
        response.setPlatformRevenueReportDto(platformRevenueSupport.buildRevenueReport());
        return response;
    }
}
