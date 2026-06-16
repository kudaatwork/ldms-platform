package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.inventory.management.business.auditable.api.WarehouseLocationServiceAuditable;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.model.WarehouseLocationType;
import projectlx.inventory.management.repository.WarehouseLocationRepository;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransitWarehouseSupport {

    private static final String TRANSIT_LOCATION_PREFIX = "VIRTUAL-TRANSIT-";

    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseLocationServiceAuditable warehouseLocationServiceAuditable;
    private final BranchAllocationSupport branchAllocationSupport;

    public WarehouseLocation resolveOrCreateTransitWarehouse(Long organizationId, Locale locale, String username) {
        Optional<WarehouseLocation> existing = warehouseLocationRepository
                .findFirstBySupplierIdAndWarehouseTypeAndVirtualWarehouseTrueAndEntityStatusNot(
                        organizationId, WarehouseLocationType.TRANSIT, EntityStatus.DELETED);
        if (existing.isPresent()) {
            return existing.get();
        }

        BranchDto headOffice = branchAllocationSupport.findHeadOfficeBranch(organizationId, locale)
                .orElseThrow(() -> new IllegalStateException(
                        "Head-office branch is required before creating in-transit warehouse for org " + organizationId));

        WarehouseLocation transit = new WarehouseLocation();
        transit.setName("In Transit");
        transit.setDescription("Virtual warehouse holding goods in transit for organisation " + organizationId);
        transit.setLocationId(TRANSIT_LOCATION_PREFIX + organizationId);
        transit.setSupplierId(organizationId);
        transit.setBranchId(headOffice.getId());
        transit.setWarehouseType(WarehouseLocationType.TRANSIT);
        transit.setVirtualWarehouse(true);
        transit.setEntityStatus(EntityStatus.ACTIVE);

        WarehouseLocation saved = warehouseLocationServiceAuditable.create(transit, locale, username);
        log.info("Created virtual TRANSIT warehouse id={} for organisation {}", saved.getId(), organizationId);
        return saved;
    }
}
