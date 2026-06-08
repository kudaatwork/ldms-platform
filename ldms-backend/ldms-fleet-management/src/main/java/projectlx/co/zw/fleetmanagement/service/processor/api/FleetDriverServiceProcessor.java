package projectlx.co.zw.fleetmanagement.service.processor.api;

import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetDriverResponse;

import java.util.Locale;

public interface FleetDriverServiceProcessor {
    FleetDriverResponse list(Locale locale, String username);
    FleetDriverResponse create(CreateFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse update(Long id, EditFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse delete(Long id, Locale locale, String username);
}
