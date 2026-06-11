package projectlx.fleet.management.service.processor.api;

import projectlx.fleet.management.utils.requests.CreateFleetDriverRequest;
import projectlx.fleet.management.utils.requests.EditFleetDriverRequest;
import projectlx.fleet.management.utils.responses.FleetDriverResponse;

import java.util.Locale;

public interface FleetDriverServiceProcessor {
    FleetDriverResponse list(Locale locale, String username);
    FleetDriverResponse create(CreateFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse update(Long id, EditFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse delete(Long id, Locale locale, String username);
}
