package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.billing.payments.model.UsageChargeRecord;
import projectlx.billing.payments.utils.enums.OrganizationBillingMode;
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
            SELECT COUNT(u) > 0 FROM UsageChargeRecord u
            WHERE u.organizationId = :organizationId
              AND u.tripId = :tripId
              AND u.actionCode IN ('TRIP_TRACK', 'GPS_PING', 'LIVE_MAP_SESSION')
              AND u.entityStatus <> :deleted
              AND u.createdAt >= :dayStart
              AND u.createdAt < :dayEnd
            """)
    boolean existsTrackingChargeForTripOnDay(
            @Param("organizationId") Long organizationId,
            @Param("tripId") Long tripId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd,
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

    @Query("""
            SELECT u.organizationId,
                   SUM(CASE WHEN u.deducted = true THEN u.chargeCents ELSE 0 END),
                   SUM(CASE WHEN u.billingMode = :subscriptionMode THEN u.chargeCents ELSE 0 END),
                   SUM(u.chargeCents),
                   COUNT(u)
            FROM UsageChargeRecord u
            WHERE u.entityStatus <> :deleted
            GROUP BY u.organizationId
            """)
    List<Object[]> aggregateUsageByOrganization(
            @Param("subscriptionMode") OrganizationBillingMode subscriptionMode,
            @Param("deleted") EntityStatus deleted);

    @Query("""
            SELECT YEAR(u.createdAt), MONTH(u.createdAt), SUM(u.chargeCents)
            FROM UsageChargeRecord u
            WHERE u.entityStatus <> :deleted
              AND u.deducted = true
              AND u.createdAt >= :from
            GROUP BY YEAR(u.createdAt), MONTH(u.createdAt)
            ORDER BY YEAR(u.createdAt), MONTH(u.createdAt)
            """)
    List<Object[]> sumDeductedChargesByMonth(
            @Param("from") LocalDateTime from,
            @Param("deleted") EntityStatus deleted);

    @Query("""
            SELECT YEAR(u.createdAt), MONTH(u.createdAt), SUM(u.chargeCents)
            FROM UsageChargeRecord u
            WHERE u.entityStatus <> :deleted
              AND u.billingMode = :subscriptionMode
              AND u.createdAt >= :from
            GROUP BY YEAR(u.createdAt), MONTH(u.createdAt)
            ORDER BY YEAR(u.createdAt), MONTH(u.createdAt)
            """)
    List<Object[]> sumSubscriptionUsageByMonth(
            @Param("from") LocalDateTime from,
            @Param("subscriptionMode") OrganizationBillingMode subscriptionMode,
            @Param("deleted") EntityStatus deleted);

    @Query("""
            SELECT u.actionCode, MAX(u.actionDisplayName), SUM(u.chargeCents), COUNT(u)
            FROM UsageChargeRecord u
            WHERE u.organizationId = :organizationId
              AND u.entityStatus <> :deleted
            GROUP BY u.actionCode
            ORDER BY SUM(u.chargeCents) DESC
            """)
    List<Object[]> sumByActionForOrganization(
            @Param("organizationId") Long organizationId,
            @Param("deleted") EntityStatus deleted);

    @Query("""
            SELECT u.actionCode, SUM(u.chargeCents)
            FROM UsageChargeRecord u
            WHERE u.entityStatus <> :deleted
              AND u.chargeCents > 0
            GROUP BY u.actionCode
            """)
    List<Object[]> sumChargesByActionCode(@Param("deleted") EntityStatus deleted);

    List<UsageChargeRecord> findTop100ByEntityStatusNotOrderByCreatedAtDesc(EntityStatus entityStatus);

    @Query("""
            SELECT COUNT(u) FROM UsageChargeRecord u
            WHERE u.organizationId = :organizationId
              AND u.actionCode IN ('NOTIFICATION_SMS', 'WHATSAPP_COMMAND')
              AND u.entityStatus <> :deleted
              AND u.createdAt >= :periodStart
              AND u.createdAt < :periodEnd
            """)
    long countMessagingUsageInPeriod(
            @Param("organizationId") Long organizationId,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("deleted") EntityStatus deleted);
}
