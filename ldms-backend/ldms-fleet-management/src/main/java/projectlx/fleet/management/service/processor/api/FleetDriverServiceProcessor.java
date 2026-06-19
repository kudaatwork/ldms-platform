package projectlx.fleet.management.service.processor.api;

import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.fleet.management.utils.responses.FleetDriverResponse;

import java.util.Locale;

public interface FleetDriverServiceProcessor {
    FleetDriverResponse list(Locale locale, String username);

    FleetDriverResponse listForTransporterPartner(Long transporterOrganizationId, Locale locale, String username);

    FleetDriverResponse create(CreateFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse update(Long id, EditFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse delete(Long id, Locale locale, String username);

    /** System/internal lookup — no organisation-workspace check. */
    FleetDriverResponse findByIdForSystem(Long id, Locale locale);

    FleetDriverResponse findByUserIdForSystem(Long userId, Locale locale);

    /** Returns the FleetDriver profile for the currently authenticated user. */
    FleetDriverResponse findMyProfile(Locale locale, String username);

    /** Marketplace: search drivers available for hire. */
    FleetDriverResponse searchMarketplace(String term, String licenseClass, Locale locale, String username);

    /** Marketplace: hire a driver from the marketplace into caller's org. */
    FleetDriverResponse hireFromMarketplace(Long driverId, Locale locale, String username);
}
