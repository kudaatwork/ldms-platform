package projectlx.fleet.management.business.auditable.api;

import projectlx.fleet.management.model.FleetDriver;

import java.util.Locale;

public interface FleetDriverServiceAuditable {
    FleetDriver create(FleetDriver fleetDriver, Locale locale, String username);
    FleetDriver update(FleetDriver fleetDriver, Locale locale, String username);
    FleetDriver delete(FleetDriver fleetDriver, Locale locale);
}
