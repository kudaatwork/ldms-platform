package projectlx.fleet.management.service.processor.impl;

import projectlx.fleet.management.business.logic.api.FleetDashboardService;
import projectlx.fleet.management.service.processor.api.FleetDashboardServiceProcessor;
import projectlx.fleet.management.utils.responses.OrganizationFleetDashboardResponse;
import projectlx.fleet.management.utils.responses.PlatformFleetDashboardResponse;

import java.util.Locale;

public class FleetDashboardServiceProcessorImpl implements FleetDashboardServiceProcessor {

    private final FleetDashboardService fleetDashboardService;

    public FleetDashboardServiceProcessorImpl(FleetDashboardService fleetDashboardService) {
        this.fleetDashboardService = fleetDashboardService;
    }

    @Override
    public OrganizationFleetDashboardResponse getOrganizationDashboard(Locale locale, String username) {
        return fleetDashboardService.getOrganizationDashboard(locale, username);
    }

    @Override
    public PlatformFleetDashboardResponse getPlatformDashboard(Locale locale) {
        return fleetDashboardService.getPlatformDashboard(locale);
    }
}
