package projectlx.fleet.management.business.logic.api;

import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.fleet.management.utils.responses.FleetDriverResponse;

import java.util.Locale;

public interface FleetDriverService {
    FleetDriverResponse list(Locale locale, String username);

    FleetDriverResponse listForTransporterPartner(Long transporterOrganizationId, Locale locale, String username);

    FleetDriverResponse create(CreateFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse update(Long id, EditFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse delete(Long id, Locale locale, String username);

    /**
     * Finds a fleet driver by ID without an organisation-workspace check.
     * Intended for system/internal callers (e.g. shipment-management, trip-tracking).
     */
    FleetDriverResponse findByIdForSystem(Long id, Locale locale);

    FleetDriverResponse findByUserIdForSystem(Long userId, Locale locale);

    /**
     * Returns the FleetDriver profile for the currently authenticated user.
     * Resolves the platform userId from the username via user-management, then looks up
     * the driver record linked to that userId.
     */
    FleetDriverResponse findMyProfile(Locale locale, String username);

    /**
     * Searches the marketplace for freelance drivers that are visible and not already
     * employed by the caller's organisation.
     *
     * @param term         optional name / licence partial match
     * @param licenseClass optional licence class filter
     * @param locale       request locale
     * @param username     caller's username
     */
    FleetDriverResponse searchMarketplace(String term, String licenseClass, Locale locale, String username);

    /**
     * Hires a marketplace driver into the caller's organisation.
     * Creates a linked FleetDriver record under the caller's org with
     * employment type {@code POOL}, pointing to the same {@code userId}.
     *
     * @param driverId the marketplace FleetDriver id
     * @param locale   request locale
     * @param username caller's username
     */
    FleetDriverResponse hireFromMarketplace(Long driverId, Locale locale, String username);
}
