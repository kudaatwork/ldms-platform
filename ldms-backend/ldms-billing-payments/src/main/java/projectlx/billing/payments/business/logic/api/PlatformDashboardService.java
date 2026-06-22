package projectlx.billing.payments.business.logic.api;

import projectlx.billing.payments.utils.responses.PlatformBillingDashboardResponse;
import projectlx.billing.payments.utils.responses.PlatformRevenueReportResponse;

import java.util.Locale;

public interface PlatformDashboardService {

    PlatformBillingDashboardResponse getBillingDashboard(Locale locale);

    PlatformRevenueReportResponse getRevenueReport(Locale locale);
}
