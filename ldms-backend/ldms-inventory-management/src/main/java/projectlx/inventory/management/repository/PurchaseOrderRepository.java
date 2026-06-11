package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseOrder> findByIdAndEntityStatusNot(Long purchaseOrderId, EntityStatus entityStatus);
}
