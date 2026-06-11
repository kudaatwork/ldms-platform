package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.inventory.management.model.WarehouseLocation;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long>, JpaSpecificationExecutor<WarehouseLocation> {
    Optional<WarehouseLocation> findByIdAndSupplierIdAndEntityStatusNot(Long warehouseLocationId, Long supplierId,
                                                                        EntityStatus entityStatus);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WarehouseLocation> findByIdAndEntityStatusNot(Long warehouseLocationId, EntityStatus entityStatus);
    List<WarehouseLocation> findByEntityStatusNotOrderByIdAsc(EntityStatus entityStatus);

    Optional<WarehouseLocation> findFirstByEntityStatusNotOrderByIdAsc(EntityStatus entityStatus);
}
