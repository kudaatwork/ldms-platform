package projectlx.fleet.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.fleet.management.business.auditable.api.FleetDriverServiceAuditable;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.fleet.management.repository.FleetDriverRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class FleetDriverServiceAuditableImpl implements FleetDriverServiceAuditable {

    private final FleetDriverRepository fleetDriverRepository;

    @Override
    public FleetDriver create(FleetDriver fleetDriver, Locale locale, String username) {
        return fleetDriverRepository.save(fleetDriver);
    }

    @Override
    public FleetDriver update(FleetDriver fleetDriver, Locale locale, String username) {
        return fleetDriverRepository.save(fleetDriver);
    }

    @Override
    public FleetDriver delete(FleetDriver fleetDriver, Locale locale) {
        return fleetDriverRepository.save(fleetDriver);
    }
}
