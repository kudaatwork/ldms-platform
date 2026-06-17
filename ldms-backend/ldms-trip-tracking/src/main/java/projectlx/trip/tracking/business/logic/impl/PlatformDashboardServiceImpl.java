package projectlx.trip.tracking.business.logic.impl;

import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.trip.tracking.business.logic.api.PlatformDashboardService;
import projectlx.trip.tracking.business.logic.support.PlatformDashboardSupport;
import projectlx.trip.tracking.utils.dtos.PlatformTripDashboardDto;
import projectlx.trip.tracking.utils.enums.I18Code;
import projectlx.trip.tracking.utils.responses.PlatformTripDashboardResponse;

import java.util.Locale;

public class PlatformDashboardServiceImpl implements PlatformDashboardService {

    private final PlatformDashboardSupport platformDashboardSupport;
    private final MessageService messageService;

    public PlatformDashboardServiceImpl(PlatformDashboardSupport platformDashboardSupport,
                                       MessageService messageService) {
        this.platformDashboardSupport = platformDashboardSupport;
        this.messageService = messageService;
    }

    @Override
    public PlatformTripDashboardResponse getTripDashboard(Locale locale) {
        PlatformTripDashboardDto dashboard = platformDashboardSupport.buildDashboardSnapshot();
        PlatformTripDashboardResponse response = new PlatformTripDashboardResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(
                I18Code.MESSAGE_TRIP_FIND_ALL_SUCCESS.getCode(), new String[]{}, locale));
        response.setPlatformTripDashboardDto(dashboard);
        return response;
    }
}
