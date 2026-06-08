package projectlx.co.zw.fleetmanagement.business.logic.api;

import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetDriverRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetDriverResponse;

import java.util.Locale;

public interface FleetDriverService {
    FleetDriverResponse list(Locale locale, String username);
    FleetDriverResponse create(CreateFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse update(Long id, EditFleetDriverRequest request, Locale locale, String username);
    FleetDriverResponse delete(Long id, Locale locale, String username);
}
