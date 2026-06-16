package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.inventory.management.model.WarehouseAccessLevel;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.inventory.management.model.WarehouseOrganizationAccess;
import projectlx.inventory.management.repository.WarehouseOrganizationAccessRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WarehouseAccessSupport {

    private final WarehouseOrganizationAccessRepository accessRepository;

    public boolean canView(WarehouseLocation warehouse, Long callerOrganizationId) {
        if (warehouse == null || callerOrganizationId == null) {
            return false;
        }
        if (callerOrganizationId.equals(warehouse.getSupplierId())) {
            return true;
        }
        return accessRepository.findByWarehouseLocationIdAndGrantedOrganizationIdAndEntityStatusNot(
                warehouse.getId(), callerOrganizationId, EntityStatus.DELETED).isPresent();
    }

    public boolean canFulfill(WarehouseLocation warehouse, Long callerOrganizationId) {
        if (warehouse == null || callerOrganizationId == null) {
            return false;
        }
        if (callerOrganizationId.equals(warehouse.getSupplierId())) {
            return true;
        }
        return accessRepository.findByWarehouseLocationIdAndGrantedOrganizationIdAndEntityStatusNot(
                        warehouse.getId(), callerOrganizationId, EntityStatus.DELETED)
                .map(a -> a.getAccessLevel() == WarehouseAccessLevel.FULFILL)
                .orElse(false);
    }

    public Set<Long> sharedWarehouseIdsForOrganization(Long organizationId) {
        Set<Long> ids = new HashSet<>();
        if (organizationId == null) {
            return ids;
        }
        List<WarehouseOrganizationAccess> grants = accessRepository
                .findByGrantedOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED);
        for (WarehouseOrganizationAccess grant : grants) {
            ids.add(grant.getWarehouseLocationId());
        }
        return ids;
    }
}
