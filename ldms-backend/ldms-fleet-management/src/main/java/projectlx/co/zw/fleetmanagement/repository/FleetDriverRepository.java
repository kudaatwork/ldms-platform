package projectlx.co.zw.fleetmanagement.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import projectlx.co.zw.fleetmanagement.model.FleetDriver;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface FleetDriverRepository extends JpaRepository<FleetDriver, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FleetDriver> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<FleetDriver> findByOrganizationIdAndEntityStatusNotOrderByIdDesc(Long organizationId, EntityStatus entityStatus);

    Optional<FleetDriver> findByIdAndOrganizationIdAndEntityStatusNot(Long id, Long organizationId, EntityStatus entityStatus);
}
