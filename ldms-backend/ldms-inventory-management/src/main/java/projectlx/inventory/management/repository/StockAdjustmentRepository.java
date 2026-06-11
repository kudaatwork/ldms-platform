package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.inventory.management.model.StockAdjustment;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long>, JpaSpecificationExecutor<StockAdjustment> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockAdjustment> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    List<StockAdjustment> findByEntityStatusNot(EntityStatus entityStatus);
}
