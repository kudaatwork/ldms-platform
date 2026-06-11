package projectlx.fleet.management.business.logic.api;

import projectlx.fleet.management.utils.requests.CompleteFleetAssetRegistrationRequest;
import projectlx.fleet.management.utils.requests.CreateFleetAssetRequest;
import projectlx.fleet.management.utils.requests.EditFleetAssetRequest;
import projectlx.fleet.management.utils.responses.FleetAssetResponse;

import java.util.Locale;

public interface FleetAssetService {
    FleetAssetResponse list(Locale locale, String username);
    FleetAssetResponse create(CreateFleetAssetRequest request, Locale locale, String username);
    FleetAssetResponse update(Long id, EditFleetAssetRequest request, Locale locale, String username);
    FleetAssetResponse delete(Long id, Locale locale, String username);
    FleetAssetResponse completeRegistration(Long id, CompleteFleetAssetRegistrationRequest request, Locale locale, String username);
}
