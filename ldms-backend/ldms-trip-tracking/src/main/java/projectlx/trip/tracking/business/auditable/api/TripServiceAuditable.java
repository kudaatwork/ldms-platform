package projectlx.trip.tracking.business.auditable.api;

import projectlx.trip.tracking.model.Trip;

import java.util.Locale;

public interface TripServiceAuditable {

    Trip create(Trip trip, Locale locale, String username);

    Trip update(Trip trip, Locale locale, String username);
}
