package projectlx.fleet.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface FleetDriverRepository extends JpaRepository<FleetDriver, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FleetDriver> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    /** Non-locking read — for system/internal callers that only need to look up driver details. */
    @Query("SELECT d FROM FleetDriver d WHERE d.id = :id AND d.entityStatus <> :excluded")
    Optional<FleetDriver> findByIdForReadOnly(
            @Param("id") Long id,
            @Param("excluded") EntityStatus excluded);

    List<FleetDriver> findByOrganizationIdAndEntityStatusNotOrderByIdDesc(Long organizationId, EntityStatus entityStatus);

    Optional<FleetDriver> findByIdAndOrganizationIdAndEntityStatusNot(Long id, Long organizationId, EntityStatus entityStatus);
}
