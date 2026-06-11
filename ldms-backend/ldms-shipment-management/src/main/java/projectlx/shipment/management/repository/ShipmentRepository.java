package projectlx.shipment.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.shipment.management.model.Shipment;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long>,
        JpaSpecificationExecutor<Shipment>, RepositoryMarkerInterface {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Shipment> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<Shipment> findByIdAndOrganizationIdAndEntityStatusNot(Long id, Long organizationId, EntityStatus entityStatus);

    Optional<Shipment> findByInventoryTransferIdAndEntityStatusNot(Long inventoryTransferId, EntityStatus entityStatus);

    List<Shipment> findByOrganizationIdAndEntityStatusNotOrderByIdDesc(Long organizationId, EntityStatus entityStatus);

    boolean existsByInventoryTransferIdAndEntityStatusNot(Long inventoryTransferId, EntityStatus entityStatus);
}
