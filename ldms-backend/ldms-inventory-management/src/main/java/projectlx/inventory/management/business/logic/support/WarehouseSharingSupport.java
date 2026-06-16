package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.inventory.management.model.WarehouseAccessLevel;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.model.WarehouseOrganizationAccess;
import projectlx.inventory.management.repository.WarehouseLocationRepository;
import projectlx.inventory.management.repository.WarehouseOrganizationAccessRepository;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WarehouseSharingSupport {

    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseOrganizationAccessRepository accessRepository;

    @Transactional
    public Optional<String> grantAccess(Long warehouseLocationId, Long grantedOrganizationId,
                                        WarehouseAccessLevel accessLevel, Locale locale, String username) {
        if (warehouseLocationId == null || grantedOrganizationId == null) {
            return Optional.of("Warehouse and organisation are required.");
        }
        WarehouseLocation warehouse = warehouseLocationRepository.findByIdAndEntityStatusNot(
                warehouseLocationId, EntityStatus.DELETED).orElse(null);
        if (warehouse == null || warehouse.isVirtualWarehouse()) {
            return Optional.of("Warehouse not found or not shareable.");
        }
        if (grantedOrganizationId.equals(warehouse.getSupplierId())) {
            return Optional.of("Organisation already owns this warehouse.");
        }
        WarehouseOrganizationAccess access = accessRepository
                .findByWarehouseLocationIdAndGrantedOrganizationIdAndEntityStatusNot(
                        warehouseLocationId, grantedOrganizationId, EntityStatus.DELETED)
                .orElseGet(WarehouseOrganizationAccess::new);
        access.setWarehouseLocationId(warehouseLocationId);
        access.setGrantedOrganizationId(grantedOrganizationId);
        access.setAccessLevel(accessLevel != null ? accessLevel : WarehouseAccessLevel.READ);
        access.setEntityStatus(EntityStatus.ACTIVE);
        access.setCreatedBy(username);
        accessRepository.save(access);
        return Optional.empty();
    }

    @Transactional
    public void revokeAccess(Long warehouseLocationId, Long grantedOrganizationId) {
        accessRepository.findByWarehouseLocationIdAndGrantedOrganizationIdAndEntityStatusNot(
                        warehouseLocationId, grantedOrganizationId, EntityStatus.DELETED)
                .ifPresent(access -> {
                    access.setEntityStatus(EntityStatus.DELETED);
                    accessRepository.save(access);
                });
    }
}
