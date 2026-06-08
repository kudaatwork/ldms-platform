package projectlx.co.zw.fleetmanagement.service.processor.api;

import projectlx.co.zw.fleetmanagement.utils.requests.CreateFleetAssetRequest;
import projectlx.co.zw.fleetmanagement.utils.requests.EditFleetAssetRequest;
import projectlx.co.zw.fleetmanagement.utils.responses.FleetAssetResponse;

import java.util.Locale;

public interface FleetAssetServiceProcessor {
    FleetAssetResponse list(Locale locale, String username);
    FleetAssetResponse create(CreateFleetAssetRequest request, Locale locale, String username);
    FleetAssetResponse update(Long id, EditFleetAssetRequest request, Locale locale, String username);
    FleetAssetResponse delete(Long id, Locale locale, String username);
}
