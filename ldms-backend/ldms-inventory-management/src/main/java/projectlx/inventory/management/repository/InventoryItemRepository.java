package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.apache.commons.csv.CSVParser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.inventory.management.model.InventoryItem;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long>, JpaSpecificationExecutor<InventoryItem> {
    Optional<InventoryItem> findByProductIdAndWarehouseLocationIdAndEntityStatusNot(Long productId, Long warehouseLocationId,
                                                                                    EntityStatus entityStatus);

    // Locked fetch by id and status
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<InventoryItem> findByEntityStatusNot(EntityStatus entityStatus);

    Optional<InventoryItem> findByWarehouseLocationIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    // Explicit locked fetch by id
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdWithLock(@Param("id") Long id);

    // Locked finder by product and warehouse (excluding a given status)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.product.id = :productId AND i.warehouseLocation.id = :warehouseLocationId AND i.entityStatus <> :entityStatus")
    Optional<InventoryItem> findByProductIdAndWarehouseLocationIdWithLock(@Param("productId") Long productId,
                                                                          @Param("warehouseLocationId") Long warehouseLocationId,
                                                                          @Param("entityStatus") EntityStatus entityStatus);

    @Query("SELECT i FROM InventoryItem i WHERE i.currentStock <= i.minStockLevel")
    List<InventoryItem> findLowStockItems();
}
