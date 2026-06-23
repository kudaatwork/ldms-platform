package projectlx.fleet.management.business.logic.api;

import projectlx.fleet.management.utils.responses.OrganizationFleetDashboardResponse;
import projectlx.fleet.management.utils.responses.PlatformFleetDashboardResponse;

import java.util.Locale;

public interface FleetDashboardService {

    OrganizationFleetDashboardResponse getOrganizationDashboard(Locale locale, String username);

    PlatformFleetDashboardResponse getPlatformDashboard(Locale locale);
}
