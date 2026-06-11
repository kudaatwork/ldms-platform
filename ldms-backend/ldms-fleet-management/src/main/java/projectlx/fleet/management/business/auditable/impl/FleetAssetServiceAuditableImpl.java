package projectlx.fleet.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.fleet.management.business.auditable.api.FleetAssetServiceAuditable;
import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.repository.FleetAssetRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class FleetAssetServiceAuditableImpl implements FleetAssetServiceAuditable {

    private final FleetAssetRepository fleetAssetRepository;

    @Override
    public FleetAsset create(FleetAsset fleetAsset, Locale locale, String username) {
        return fleetAssetRepository.save(fleetAsset);
    }

    @Override
    public FleetAsset update(FleetAsset fleetAsset, Locale locale, String username) {
        return fleetAssetRepository.save(fleetAsset);
    }

    @Override
    public FleetAsset delete(FleetAsset fleetAsset, Locale locale) {
        return fleetAssetRepository.save(fleetAsset);
    }
}
