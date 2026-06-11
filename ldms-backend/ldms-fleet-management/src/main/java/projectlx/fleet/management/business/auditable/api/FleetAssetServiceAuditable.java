package projectlx.fleet.management.business.auditable.api;

import projectlx.fleet.management.model.FleetAsset;

import java.util.Locale;

public interface FleetAssetServiceAuditable {
    FleetAsset create(FleetAsset fleetAsset, Locale locale, String username);
    FleetAsset update(FleetAsset fleetAsset, Locale locale, String username);
    FleetAsset delete(FleetAsset fleetAsset, Locale locale);
}
