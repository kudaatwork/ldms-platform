package projectlx.co.zw.fleetmanagement.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.fleetmanagement.business.logic.api.FleetDriverService;
import projectlx.co.zw.fleetmanagement.service.processor.api.FleetDriverServiceProcessor;
import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetDriverResponse;

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
    public FleetDriverResponse delete(Long id, Locale locale, String username) {
        log.info("Processing delete fleet driver {} for user {}", id, username);
        return fleetDriverService.delete(id, locale, username);
    }
}
