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

    Optional<Shipment> findBySalesOrderIdAndEntityStatusNot(Long salesOrderId, EntityStatus entityStatus);

    List<Shipment> findByOrganizationIdAndEntityStatusNotOrderByIdDesc(Long organizationId, EntityStatus entityStatus);

    List<Shipment> findByTransportCompanyOrganizationIdAndEntityStatusNotOrderByIdDesc(
            Long transportCompanyOrganizationId, EntityStatus entityStatus);

    List<Shipment> findByOrganizationIdAndStatusAndTransportCompanyOrganizationIdAndEntityStatusNotOrderByIdAsc(
            Long organizationId,
            projectlx.shipment.management.utils.enums.ShipmentStatus status,
            Long transportCompanyOrganizationId,
            EntityStatus entityStatus);

    boolean existsByInventoryTransferIdAndEntityStatusNot(Long inventoryTransferId, EntityStatus entityStatus);

    boolean existsBySalesOrderIdAndEntityStatusNot(Long salesOrderId, EntityStatus entityStatus);
}
