package projectlx.trip.tracking.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.trip.tracking.business.auditable.api.TripEventServiceAuditable;
import projectlx.trip.tracking.model.TripEvent;
import projectlx.trip.tracking.repository.TripEventRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class TripEventServiceAuditableImpl implements TripEventServiceAuditable {

    private final TripEventRepository tripEventRepository;

    @Override
    public TripEvent create(TripEvent tripEvent, Locale locale, String username) {
        return tripEventRepository.save(tripEvent);
    }
}
