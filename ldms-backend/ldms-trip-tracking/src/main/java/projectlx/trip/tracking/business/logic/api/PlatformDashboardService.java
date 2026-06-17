package projectlx.trip.tracking.business.logic.api;

import projectlx.trip.tracking.utils.responses.PlatformTripDashboardResponse;

import java.util.Locale;

public interface PlatformDashboardService {

    PlatformTripDashboardResponse getTripDashboard(Locale locale);
}
