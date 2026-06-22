package projectlx.fleet.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.fleet.management.business.logic.api.FleetDriverService;
import projectlx.fleet.management.service.processor.api.FleetDriverServiceProcessor;
import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.fleet.management.utils.requests.ProvisionFleetDriverPlatformAccessRequest;
import projectlx.fleet.management.utils.responses.FleetDriverResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class FleetDriverServiceProcessorImpl implements FleetDriverServiceProcessor {

    private final FleetDriverService fleetDriverService;

    @Override
    public FleetDriverResponse list(Locale locale, String username) {
        log.info("Processing list fleet drivers for user {}", username);
        return fleetDriverService.list(locale, username);
    }

    @Override
    public FleetDriverResponse listForTransporterPartner(Long transporterOrganizationId, Locale locale, String username) {
        log.info("Processing list transporter partner {} drivers for user {}", transporterOrganizationId, username);
        return fleetDriverService.listForTransporterPartner(transporterOrganizationId, locale, username);
    }

    @Override
    public FleetDriverResponse create(CreateFleetDriverRequest request, Locale locale, String username) {
        log.info("Processing create fleet driver for user {}", username);
        return fleetDriverService.create(request, locale, username);
    }

    @Override
    public FleetDriverResponse update(Long id, EditFleetDriverRequest request, Locale locale, String username) {
        log.info("Processing update fleet driver {} for user {}", id, username);
        return fleetDriverService.update(id, request, locale, username);
    }

    @Override
    public FleetDriverResponse provisionPlatformAccess(Long id, ProvisionFleetDriverPlatformAccessRequest request,
                                                       Locale locale, String username) {
        log.info("Processing provision platform access for fleet driver {} by {}", id, username);
        return fleetDriverService.provisionPlatformAccess(id, request, locale, username);
    }

    @Override
    public FleetDriverResponse delete(Long id, Locale locale, String username) {
        log.info("Processing delete fleet driver {} for user {}", id, username);
        return fleetDriverService.delete(id, locale, username);
    }

    @Override
    public FleetDriverResponse findByIdForSystem(Long id, Locale locale) {
        log.info("Processing system find-by-id for fleet driver {}", id);
        return fleetDriverService.findByIdForSystem(id, locale);
    }

    @Override
    public FleetDriverResponse findByUserIdForSystem(Long userId, Locale locale) {
        log.info("Processing system find-by-user-id for fleet driver userId={}", userId);
        return fleetDriverService.findByUserIdForSystem(userId, locale);
    }

    @Override
    public FleetDriverResponse findMyProfile(Locale locale, String username) {
        log.info("Processing findMyProfile for user {}", username);
        return fleetDriverService.findMyProfile(locale, username);
    }

    @Override
    public FleetDriverResponse searchMarketplace(String term, String licenseClass, Locale locale, String username) {
        log.info("Processing marketplace search term='{}' licenseClass='{}' by {}", term, licenseClass, username);
        return fleetDriverService.searchMarketplace(term, licenseClass, locale, username);
    }

    @Override
    public FleetDriverResponse hireFromMarketplace(Long driverId, Locale locale, String username) {
        log.info("Processing hire driverId={} by {}", driverId, username);
        return fleetDriverService.hireFromMarketplace(driverId, locale, username);
    }
}
