package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.inventory.management.model.WarehouseOrganizationAccess;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface WarehouseOrganizationAccessRepository extends JpaRepository<WarehouseOrganizationAccess, Long> {

    List<WarehouseOrganizationAccess> findByGrantedOrganizationIdAndEntityStatusNot(
            Long grantedOrganizationId, EntityStatus entityStatus);

    Optional<WarehouseOrganizationAccess> findByWarehouseLocationIdAndGrantedOrganizationIdAndEntityStatusNot(
            Long warehouseLocationId, Long grantedOrganizationId, EntityStatus entityStatus);

    List<WarehouseOrganizationAccess> findByWarehouseLocationIdAndEntityStatusNot(
            Long warehouseLocationId, EntityStatus entityStatus);
}
