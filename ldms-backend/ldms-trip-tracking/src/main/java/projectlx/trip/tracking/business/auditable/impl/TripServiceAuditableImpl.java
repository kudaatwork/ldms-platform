package projectlx.trip.tracking.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.trip.tracking.business.auditable.api.TripServiceAuditable;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.repository.TripRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class TripServiceAuditableImpl implements TripServiceAuditable {

    private final TripRepository tripRepository;

    @Override
    public Trip create(Trip trip, Locale locale, String username) {
        return tripRepository.save(trip);
    }

    @Override
    public Trip update(Trip trip, Locale locale, String username) {
        return tripRepository.save(trip);
    }
}
