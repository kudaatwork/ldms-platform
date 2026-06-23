package projectlx.fleet.management.business.logic.impl;

import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.fleet.management.business.logic.api.FleetDashboardService;
import projectlx.fleet.management.business.logic.support.CallerOrganizationResolver;
import projectlx.fleet.management.business.logic.support.FleetDashboardSupport;
import projectlx.fleet.management.utils.dtos.OrganizationFleetDashboardDto;
import projectlx.fleet.management.utils.dtos.PlatformFleetDashboardDto;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.fleet.management.utils.responses.OrganizationFleetDashboardResponse;
import projectlx.fleet.management.utils.responses.PlatformFleetDashboardResponse;

import java.util.Locale;

public class FleetDashboardServiceImpl implements FleetDashboardService {

    private final FleetDashboardSupport fleetDashboardSupport;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final MessageService messageService;

    public FleetDashboardServiceImpl(FleetDashboardSupport fleetDashboardSupport,
                                     CallerOrganizationResolver callerOrganizationResolver,
                                     MessageService messageService) {
        this.fleetDashboardSupport = fleetDashboardSupport;
        this.callerOrganizationResolver = callerOrganizationResolver;
        this.messageService = messageService;
    }

    @Override
    public OrganizationFleetDashboardResponse getOrganizationDashboard(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            OrganizationFleetDashboardResponse response = new OrganizationFleetDashboardResponse();
            response.setSuccess(false);
            response.setStatusCode(400);
            response.setMessage(messageService.getMessage(
                    I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale));
            return response;
        }
        OrganizationFleetDashboardDto dashboard = fleetDashboardSupport.buildOrganizationSnapshot(organizationId);
        OrganizationFleetDashboardResponse response = new OrganizationFleetDashboardResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(
                I18Code.MESSAGE_FLEET_DASHBOARD_SUCCESS.getCode(), new String[]{}, locale));
        response.setOrganizationFleetDashboardDto(dashboard);
        return response;
    }

    @Override
    public PlatformFleetDashboardResponse getPlatformDashboard(Locale locale) {
        PlatformFleetDashboardDto dashboard = fleetDashboardSupport.buildPlatformSnapshot();
        PlatformFleetDashboardResponse response = new PlatformFleetDashboardResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(
                I18Code.MESSAGE_FLEET_DASHBOARD_SUCCESS.getCode(), new String[]{}, locale));
        response.setPlatformFleetDashboardDto(dashboard);
        return response;
    }
}
