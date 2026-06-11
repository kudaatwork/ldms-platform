package projectlx.trip.tracking.business.auditable.api;

import projectlx.trip.tracking.model.TripEvent;

import java.util.Locale;

public interface TripEventServiceAuditable {

    TripEvent create(TripEvent tripEvent, Locale locale, String username);
}
