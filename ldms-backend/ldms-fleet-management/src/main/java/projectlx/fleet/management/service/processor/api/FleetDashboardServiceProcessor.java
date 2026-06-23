package projectlx.fleet.management.service.processor.api;

import projectlx.fleet.management.utils.responses.OrganizationFleetDashboardResponse;
import projectlx.fleet.management.utils.responses.PlatformFleetDashboardResponse;

import java.util.Locale;

public interface FleetDashboardServiceProcessor {

    OrganizationFleetDashboardResponse getOrganizationDashboard(Locale locale, String username);

    PlatformFleetDashboardResponse getPlatformDashboard(Locale locale);
}
