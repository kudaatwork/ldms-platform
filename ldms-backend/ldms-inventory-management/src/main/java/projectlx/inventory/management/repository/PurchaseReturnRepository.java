package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.inventory.management.model.PurchaseReturn;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long>, JpaSpecificationExecutor<PurchaseReturn> {
    Optional<PurchaseReturn> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    List<PurchaseReturn> findByEntityStatusNot(EntityStatus entityStatus);
}