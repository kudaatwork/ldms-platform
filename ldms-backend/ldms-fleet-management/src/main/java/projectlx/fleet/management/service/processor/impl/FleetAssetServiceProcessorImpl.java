package projectlx.fleet.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.fleet.management.business.logic.api.FleetAssetService;
import projectlx.fleet.management.service.processor.api.FleetAssetServiceProcessor;
import projectlx.fleet.management.utils.requests.CompleteFleetAssetRegistrationRequest;
import projectlx.fleet.management.utils.requests.CreateFleetAssetRequest;
import projectlx.fleet.management.utils.requests.EditFleetAssetRequest;
import projectlx.fleet.management.utils.responses.FleetAssetResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class FleetAssetServiceProcessorImpl implements FleetAssetServiceProcessor {

    private final FleetAssetService fleetAssetService;

    @Override
    public FleetAssetResponse list(Locale locale, String username) {
        log.info("Processing list fleet assets for user {}", username);
        return fleetAssetService.list(locale, username);
    }

    @Override
    public FleetAssetResponse create(CreateFleetAssetRequest request, Locale locale, String username) {
        log.info("Processing create fleet asset for user {}", username);
        return fleetAssetService.create(request, locale, username);
    }

    @Override
    public FleetAssetResponse update(Long id, EditFleetAssetRequest request, Locale locale, String username) {
        log.info("Processing update fleet asset {} for user {}", id, username);
        return fleetAssetService.update(id, request, locale, username);
    }

    @Override
    public FleetAssetResponse delete(Long id, Locale locale, String username) {
        log.info("Processing delete fleet asset {} for user {}", id, username);
        return fleetAssetService.delete(id, locale, username);
    }

    @Override
    public FleetAssetResponse completeRegistration(Long id, CompleteFleetAssetRegistrationRequest request, Locale locale, String username) {
        log.info("Processing complete-registration for fleet asset {} by user {}", id, username);
        return fleetAssetService.completeRegistration(id, request, locale, username);
    }
}
