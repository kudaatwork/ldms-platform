package projectlx.billing.payments.service.processor.api;

import projectlx.billing.payments.utils.responses.PlatformBillingDashboardResponse;
import projectlx.billing.payments.utils.responses.PlatformRevenueReportResponse;

import java.util.Locale;

public interface PlatformDashboardServiceProcessor {

    PlatformBillingDashboardResponse getBillingDashboard(Locale locale);

    PlatformRevenueReportResponse getRevenueReport(Locale locale);
}
