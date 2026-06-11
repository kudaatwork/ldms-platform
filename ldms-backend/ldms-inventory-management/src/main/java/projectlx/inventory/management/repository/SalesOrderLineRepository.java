package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.inventory.management.model.SalesOrderLine;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long>, JpaSpecificationExecutor<SalesOrderLine> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SalesOrderLine> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<SalesOrderLine> findByEntityStatusNot(EntityStatus entityStatus);
}
