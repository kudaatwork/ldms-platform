package projectlx.fleet.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.fleet.management.business.logic.api.FleetTrackingIntegrationCredentialService;
import projectlx.fleet.management.service.processor.api.FleetTrackingIntegrationCredentialServiceProcessor;
import projectlx.fleet.management.utils.requests.CreateFleetTrackingIntegrationCredentialRequest;
import projectlx.fleet.management.utils.responses.FleetTrackingIntegrationCredentialResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class FleetTrackingIntegrationCredentialServiceProcessorImpl
        implements FleetTrackingIntegrationCredentialServiceProcessor {

    private final FleetTrackingIntegrationCredentialService fleetTrackingIntegrationCredentialService;

    @Override
    public FleetTrackingIntegrationCredentialResponse create(
            CreateFleetTrackingIntegrationCredentialRequest request, Locale locale, String username) {
        log.info("Processing create tracking integration credential for user {}", username);
        return fleetTrackingIntegrationCredentialService.create(request, locale, username);
    }

    @Override
    public FleetTrackingIntegrationCredentialResponse findAllByOrganization(
            Long organizationId, Locale locale, String username) {
        log.info("Processing list tracking integration credentials for org {} user {}", organizationId, username);
        return fleetTrackingIntegrationCredentialService.findAllByOrganization(organizationId, locale, username);
    }

    @Override
    public FleetTrackingIntegrationCredentialResponse findById(Long id, Locale locale, String username) {
        log.info("Processing find tracking integration credential id={} for user {}", id, username);
        return fleetTrackingIntegrationCredentialService.findById(id, locale, username);
    }

    @Override
    public FleetTrackingIntegrationCredentialResponse suspend(Long id, Locale locale, String username) {
        log.info("Processing suspend tracking integration credential id={} for user {}", id, username);
        return fleetTrackingIntegrationCredentialService.suspend(id, locale, username);
    }

    @Override
    public FleetTrackingIntegrationCredentialResponse delete(Long id, Locale locale, String username) {
        log.info("Processing delete tracking integration credential id={} for user {}", id, username);
        return fleetTrackingIntegrationCredentialService.delete(id, locale, username);
    }
}
