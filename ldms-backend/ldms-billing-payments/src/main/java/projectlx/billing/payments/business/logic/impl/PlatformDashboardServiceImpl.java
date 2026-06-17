package projectlx.billing.payments.business.logic.impl;

import projectlx.billing.payments.business.logic.api.PlatformDashboardService;
import projectlx.billing.payments.business.logic.support.PlatformDashboardSupport;
import projectlx.billing.payments.utils.dtos.PlatformBillingDashboardDto;
import projectlx.billing.payments.utils.responses.PlatformBillingDashboardResponse;

import java.util.Locale;

public class PlatformDashboardServiceImpl implements PlatformDashboardService {

    private final PlatformDashboardSupport platformDashboardSupport;

    public PlatformDashboardServiceImpl(PlatformDashboardSupport platformDashboardSupport) {
        this.platformDashboardSupport = platformDashboardSupport;
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
}
