package projectlx.fleet.management.business.auditable.api;

import projectlx.fleet.management.model.FleetDriverSignupRequest;

import java.util.Locale;

public interface FleetDriverSignupRequestServiceAuditable {

    FleetDriverSignupRequest create(FleetDriverSignupRequest request, Locale locale, String username);

    FleetDriverSignupRequest update(FleetDriverSignupRequest request, Locale locale, String username);
}
