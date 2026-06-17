package projectlx.trip.tracking.service.processor.impl;

import projectlx.trip.tracking.business.logic.api.PlatformDashboardService;
import projectlx.trip.tracking.service.processor.api.PlatformDashboardServiceProcessor;
import projectlx.trip.tracking.utils.responses.PlatformTripDashboardResponse;

import java.util.Locale;

public class PlatformDashboardServiceProcessorImpl implements PlatformDashboardServiceProcessor {

    private final PlatformDashboardService platformDashboardService;

    public PlatformDashboardServiceProcessorImpl(PlatformDashboardService platformDashboardService) {
        this.platformDashboardService = platformDashboardService;
    }

    @Override
    public PlatformTripDashboardResponse getTripDashboard(Locale locale) {
        return platformDashboardService.getTripDashboard(locale);
    }
}
