package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.billing.payments.model.UsageChargeRecord;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface UsageChargeRecordRepository extends JpaRepository<UsageChargeRecord, Long> {

    List<UsageChargeRecord> findByOrganizationIdAndTripIdAndEntityStatusNotOrderByCreatedAtAsc(
            Long organizationId, Long tripId, EntityStatus entityStatus);

    List<UsageChargeRecord> findByOrganizationIdAndSeasonIdAndEntityStatusNotOrderByCreatedAtAsc(
            Long organizationId, Long seasonId, EntityStatus entityStatus);

    @Query("""
            SELECT u FROM UsageChargeRecord u
            WHERE u.organizationId = :organizationId
              AND u.entityStatus <> :deleted
              AND u.createdAt >= :from
              AND u.createdAt < :to
              AND (:tripId IS NULL OR u.tripId = :tripId)
              AND (:seasonId IS NULL OR u.seasonId = :seasonId)
            ORDER BY u.createdAt ASC
            """)
    List<UsageChargeRecord> findForReport(
            @Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("tripId") Long tripId,
            @Param("seasonId") Long seasonId,
            @Param("deleted") EntityStatus deleted);

    @Query("""
            SELECT u.actionCode, SUM(u.chargeCents)
            FROM UsageChargeRecord u
            WHERE u.organizationId = :organizationId
              AND u.entityStatus <> :deleted
              AND u.createdAt >= :from
              AND u.createdAt < :to
              AND (:tripId IS NULL OR u.tripId = :tripId)
              AND (:seasonId IS NULL OR u.seasonId = :seasonId)
            GROUP BY u.actionCode
            ORDER BY SUM(u.chargeCents) DESC
            """)
    List<Object[]> sumByActionForReport(
            @Param("organizationId") Long organizationId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("tripId") Long tripId,
            @Param("seasonId") Long seasonId,
            @Param("deleted") EntityStatus deleted);
}
