package projectlx.trip.tracking.service.processor.api;

import projectlx.trip.tracking.utils.responses.PlatformTripDashboardResponse;

import java.util.Locale;

public interface PlatformDashboardServiceProcessor {

    PlatformTripDashboardResponse getTripDashboard(Locale locale);
}
