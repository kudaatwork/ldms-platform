package projectlx.fleet.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.fleet.management.business.auditable.api.FleetDriverSignupRequestServiceAuditable;
import projectlx.fleet.management.model.FleetDriverSignupRequest;
import projectlx.fleet.management.repository.FleetDriverSignupRequestRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class FleetDriverSignupRequestServiceAuditableImpl implements FleetDriverSignupRequestServiceAuditable {

    private final FleetDriverSignupRequestRepository repository;

    @Override
    public FleetDriverSignupRequest create(FleetDriverSignupRequest request, Locale locale, String username) {
        return repository.save(request);
    }

    @Override
    public FleetDriverSignupRequest update(FleetDriverSignupRequest request, Locale locale, String username) {
        return repository.save(request);
    }
}
