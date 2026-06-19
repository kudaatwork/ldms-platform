package projectlx.fleet.management.service.processor.api;

import projectlx.fleet.management.utils.requests.CreateFleetTrackingIntegrationCredentialRequest;
import projectlx.fleet.management.utils.responses.FleetTrackingIntegrationCredentialResponse;

import java.util.Locale;

public interface FleetTrackingIntegrationCredentialServiceProcessor {

    FleetTrackingIntegrationCredentialResponse create(
            CreateFleetTrackingIntegrationCredentialRequest request, Locale locale, String username);

    FleetTrackingIntegrationCredentialResponse findAllByOrganization(
            Long organizationId, Locale locale, String username);

    FleetTrackingIntegrationCredentialResponse findById(Long id, Locale locale, String username);

    FleetTrackingIntegrationCredentialResponse suspend(Long id, Locale locale, String username);

    FleetTrackingIntegrationCredentialResponse delete(Long id, Locale locale, String username);
}
