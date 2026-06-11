package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.inventory.management.model.PurchaseOrderLine;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long>, JpaSpecificationExecutor<PurchaseOrderLine> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseOrderLine> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    List<PurchaseOrderLine> findByEntityStatusNot(EntityStatus entityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM PurchaseOrderLine l WHERE l.id = :id")
    Optional<PurchaseOrderLine> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(MAX(l.lineNumber), 0) FROM PurchaseOrderLine l WHERE l.purchaseOrder.id = :purchaseOrderId")
    Integer findMaxLineNumberByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);
}
