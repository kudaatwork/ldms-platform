package projectlx.fleet.management.service.processor.api;

import projectlx.fleet.management.utils.requests.AssignFleetAssetDriverRequest;
import projectlx.fleet.management.utils.requests.CompleteFleetAssetRegistrationRequest;
import projectlx.fleet.management.utils.requests.CreateFleetAssetRequest;
import projectlx.fleet.management.utils.requests.EditFleetAssetRequest;
import projectlx.fleet.management.utils.responses.FleetAssetResponse;

import java.util.Locale;

public interface FleetAssetServiceProcessor {
    FleetAssetResponse list(Locale locale, String username);
    FleetAssetResponse create(CreateFleetAssetRequest request, Locale locale, String username);
    FleetAssetResponse update(Long id, EditFleetAssetRequest request, Locale locale, String username);
    FleetAssetResponse assignDriver(Long id, AssignFleetAssetDriverRequest request, Locale locale, String username);
    FleetAssetResponse delete(Long id, Locale locale, String username);
    FleetAssetResponse completeRegistration(Long id, CompleteFleetAssetRegistrationRequest request, Locale locale, String username);
    FleetAssetResponse findByIdForSystem(Long id, Locale locale);
}
