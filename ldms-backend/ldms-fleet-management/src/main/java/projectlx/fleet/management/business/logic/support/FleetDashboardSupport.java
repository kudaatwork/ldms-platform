package projectlx.fleet.management.business.logic.support;

import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.fleet.management.repository.FleetAssetRepository;
import projectlx.fleet.management.repository.FleetDriverRepository;
import projectlx.fleet.management.utils.dtos.OrganizationFleetDashboardDto;
import projectlx.fleet.management.utils.dtos.PlatformFleetDashboardDto;
import projectlx.fleet.management.utils.enums.DriverEmploymentType;
import projectlx.fleet.management.utils.enums.FleetOwnershipType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FleetDashboardSupport {

    private final FleetAssetRepository fleetAssetRepository;
    private final FleetDriverRepository fleetDriverRepository;

    public FleetDashboardSupport(FleetAssetRepository fleetAssetRepository,
                                 FleetDriverRepository fleetDriverRepository) {
        this.fleetAssetRepository = fleetAssetRepository;
        this.fleetDriverRepository = fleetDriverRepository;
    }

    public OrganizationFleetDashboardDto buildOrganizationSnapshot(Long organizationId) {
        OrganizationFleetDashboardDto dto = new OrganizationFleetDashboardDto();
        dto.setOwnedFleetCount(fleetAssetRepository.countByOrganizationIdAndOwnershipTypeAndEntityStatusNot(
                organizationId, FleetOwnershipType.OWNED, EntityStatus.DELETED));
        dto.setContractedFleetCount(fleetAssetRepository.countByOrganizationIdAndOwnershipTypeAndEntityStatusNot(
                organizationId, FleetOwnershipType.CONTRACTED, EntityStatus.DELETED));
        dto.setOrganizationDriverCount(fleetDriverRepository.countByOrganizationIdAndEntityStatusNot(
                organizationId, EntityStatus.DELETED));
        dto.setContractedDriverCount(countContractedDrivers(organizationId));
        return dto;
    }

    public PlatformFleetDashboardDto buildPlatformSnapshot() {
        PlatformFleetDashboardDto dto = new PlatformFleetDashboardDto();
        dto.setTotalFleetAssets(fleetAssetRepository.countByEntityStatusNot(EntityStatus.DELETED));
        dto.setOwnedFleetAssets(fleetAssetRepository.countByOwnershipTypeAndEntityStatusNot(
                FleetOwnershipType.OWNED, EntityStatus.DELETED));
        dto.setContractedFleetAssets(fleetAssetRepository.countByOwnershipTypeAndEntityStatusNot(
                FleetOwnershipType.CONTRACTED, EntityStatus.DELETED));
        dto.setTotalDrivers(fleetDriverRepository.countByEntityStatusNot(EntityStatus.DELETED));
        dto.setOrganizationsWithFleet(fleetAssetRepository.countDistinctOrganizationIdByEntityStatusNot(
                EntityStatus.DELETED));
        return dto;
    }

    private long countContractedDrivers(Long organizationId) {
        Set<Long> contractedDriverIds = new HashSet<>();
        List<FleetDriver> poolDrivers = fleetDriverRepository
                .findByOrganizationIdAndEmploymentTypeAndEntityStatusNotOrderByIdDesc(
                        organizationId, DriverEmploymentType.POOL, EntityStatus.DELETED);
        for (FleetDriver driver : poolDrivers) {
            contractedDriverIds.add(driver.getId());
        }

        List<Long> transporterOrganizationIds = fleetAssetRepository
                .findDistinctContractedTransporterOrganizationIds(
                        organizationId, FleetOwnershipType.CONTRACTED, EntityStatus.DELETED);
        if (!transporterOrganizationIds.isEmpty()) {
            List<FleetDriver> partnerDrivers = fleetDriverRepository
                    .findByOrganizationIdInAndEntityStatusNotOrderByIdDesc(transporterOrganizationIds, EntityStatus.DELETED);
            for (FleetDriver driver : partnerDrivers) {
                contractedDriverIds.add(driver.getId());
            }
        }
        return contractedDriverIds.size();
    }
}
