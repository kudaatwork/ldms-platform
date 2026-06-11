package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import projectlx.inventory.management.model.InventoryTransfer;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface InventoryTransferRepository extends JpaRepository<InventoryTransfer, Long>, JpaSpecificationExecutor<InventoryTransfer> {
    List<InventoryTransfer> findByEntityStatusNot(EntityStatus entityStatus);

    @Query("""
            SELECT DISTINCT t FROM InventoryTransfer t
            LEFT JOIN FETCH t.product
            LEFT JOIN FETCH t.fromLocation
            LEFT JOIN FETCH t.toLocation
            WHERE t.entityStatus <> :entityStatus
            """)
    List<InventoryTransfer> findAllActiveWithDetails(EntityStatus entityStatus);

    @Query("""
            SELECT t FROM InventoryTransfer t
            LEFT JOIN FETCH t.product
            LEFT JOIN FETCH t.fromLocation
            LEFT JOIN FETCH t.toLocation
            WHERE t.id = :id AND t.entityStatus <> :entityStatus
            """)
    Optional<InventoryTransfer> findByIdAndEntityStatusNotWithDetails(Long id, EntityStatus entityStatus);

    Optional<InventoryTransfer> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
}
