package projectlx.fleet.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.fleet.management.clients.ShipmentManagementServiceClient;
import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.utils.requests.AutoAllocateShipmentFromFleetRequest;
import projectlx.fleet.management.utils.responses.ShipmentFeignResponse;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class FleetShipmentAutoAllocationSupport {

    private final ShipmentManagementServiceClient shipmentManagementServiceClient;

    public void tryAutoAllocate(FleetAsset asset, Locale locale) {
        if (asset == null || asset.getFleetDriverId() == null || asset.getFleetDriverId() < 1) {
            return;
        }
        try {
            AutoAllocateShipmentFromFleetRequest request = new AutoAllocateShipmentFromFleetRequest();
            request.setFleetAssetId(asset.getId());
            request.setFleetDriverId(asset.getFleetDriverId());
            request.setAssetOrganizationId(asset.getOrganizationId());
            request.setOwnershipType(asset.getOwnershipType() != null ? asset.getOwnershipType().name() : null);
            request.setContractedTransporterOrganizationId(asset.getContractedTransporterOrganizationId());
            ShipmentFeignResponse response = shipmentManagementServiceClient.autoAllocateFromFleet(request, locale);
            if (response != null && response.isSuccess()) {
                log.info("Synced fleet assignment to shipment for assetId={} driverId={}",
                        asset.getId(), asset.getFleetDriverId());
            }
        } catch (Exception ex) {
            log.warn("Shipment auto-allocation skipped for assetId={}: {}", asset.getId(), ex.getMessage());
        }
    }
}
