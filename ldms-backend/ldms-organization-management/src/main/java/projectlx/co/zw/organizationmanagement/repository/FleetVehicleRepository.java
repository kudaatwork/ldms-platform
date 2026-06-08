package projectlx.co.zw.organizationmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.co.zw.organizationmanagement.model.FleetVehicle;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, Long> {

    List<FleetVehicle> findByOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(
            Long organizationId, EntityStatus entityStatus);

    List<FleetVehicle> findByOrganizationIdAndOwnershipTypeAndEntityStatusNotOrderByCreatedAtDesc(
            Long organizationId, String ownershipType, EntityStatus entityStatus);

    List<FleetVehicle> findByOrganizationIdAndContractedTransporterOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(
            Long organizationId, Long contractedTransporterOrganizationId, EntityStatus entityStatus);

    Optional<FleetVehicle> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
}
