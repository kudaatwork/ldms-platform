package projectlx.shipment.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.shipment.management.model.BorderClearanceCase;
import projectlx.shipment.management.utils.enums.BorderClearanceStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface BorderClearanceCaseRepository extends JpaRepository<BorderClearanceCase, Long>,
        JpaSpecificationExecutor<BorderClearanceCase> {

    Optional<BorderClearanceCase> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<BorderClearanceCase> findByShipmentIdAndEntityStatusNot(Long shipmentId, EntityStatus entityStatus);

    List<BorderClearanceCase> findAllByOrganizationIdAndEntityStatusNot(Long organizationId, EntityStatus entityStatus);

    List<BorderClearanceCase> findAllByOrganizationIdAndStatusAndEntityStatusNot(
            Long organizationId, BorderClearanceStatus status, EntityStatus entityStatus);

    List<BorderClearanceCase> findAllByShipmentIdAndEntityStatusNot(Long shipmentId, EntityStatus entityStatus);

    boolean existsByShipmentIdAndEntityStatusNot(Long shipmentId, EntityStatus entityStatus);
}
