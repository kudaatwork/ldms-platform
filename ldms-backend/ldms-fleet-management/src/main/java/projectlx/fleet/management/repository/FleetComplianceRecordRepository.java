package projectlx.fleet.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.fleet.management.model.FleetComplianceRecord;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FleetComplianceRecordRepository extends JpaRepository<FleetComplianceRecord, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FleetComplianceRecord> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<FleetComplianceRecord> findByOrganizationIdAndEntityStatusNotOrderByIdDesc(
            Long organizationId, EntityStatus entityStatus);

    Optional<FleetComplianceRecord> findByIdAndOrganizationIdAndEntityStatusNot(
            Long id, Long organizationId, EntityStatus entityStatus);

    @Query("""
            SELECT c FROM FleetComplianceRecord c
            WHERE c.organizationId = :organizationId
              AND c.entityStatus <> :deleted
              AND c.expiresAt IS NOT NULL
              AND c.expiresAt <= :threshold
            ORDER BY c.expiresAt ASC
            """)
    List<FleetComplianceRecord> findExpiringByOrganizationId(
            @Param("organizationId") Long organizationId,
            @Param("threshold") LocalDateTime threshold,
            @Param("deleted") EntityStatus deleted);
}
