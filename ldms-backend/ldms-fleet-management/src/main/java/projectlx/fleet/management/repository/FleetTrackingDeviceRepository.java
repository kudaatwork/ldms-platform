package projectlx.fleet.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fleet.management.model.FleetTrackingDevice;
import projectlx.fleet.management.utils.enums.TrackingIntegrationProvider;

import java.util.List;
import java.util.Optional;

public interface FleetTrackingDeviceRepository extends JpaRepository<FleetTrackingDevice, Long>,
        JpaSpecificationExecutor<FleetTrackingDevice> {

    List<FleetTrackingDevice> findByOrganizationIdAndEntityStatusNotOrderByIdDesc(
            Long organizationId, EntityStatus entityStatus);

    List<FleetTrackingDevice> findByOrganizationIdAndIntegrationProviderNotAndEntityStatusNotOrderByIdDesc(
            Long organizationId, TrackingIntegrationProvider integrationProvider, EntityStatus entityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FleetTrackingDevice> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<FleetTrackingDevice> findByIngestKeyAndEntityStatusNot(String ingestKey, EntityStatus entityStatus);
}
