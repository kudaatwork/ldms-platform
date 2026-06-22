package projectlx.trip.tracking.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.repository.projection.OrganizationTripStatsProjection;
import projectlx.trip.tracking.utils.enums.TripStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Trip> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    @Query("SELECT t FROM Trip t WHERE t.id = :id AND t.entityStatus <> :entityStatus")
    Optional<Trip> findByIdAndEntityStatusNotNoLock(@Param("id") Long id, @Param("entityStatus") EntityStatus entityStatus);

    Optional<Trip> findByTripNumberAndEntityStatusNot(String tripNumber, EntityStatus entityStatus);

    Optional<Trip> findByShipmentIdAndStatusInAndEntityStatusNot(
            Long shipmentId, List<TripStatus> statuses, EntityStatus entityStatus);

    boolean existsByShipmentIdAndStatusNotInAndEntityStatusNot(
            Long shipmentId, List<TripStatus> excludedStatuses, EntityStatus entityStatus);

    List<Trip> findByShipmentIdAndStatusNotInAndEntityStatusNotOrderByIdDesc(
            Long shipmentId, List<TripStatus> excludedStatuses, EntityStatus entityStatus);

    List<Trip> findByFleetDriverIdAndEntityStatusNotOrderByStartedAtDesc(
            Long fleetDriverId, EntityStatus entityStatus);

    Page<Trip> findByOrganizationIdAndEntityStatusNot(Long organizationId, EntityStatus entityStatus, Pageable pageable);

    Page<Trip> findByOrganizationIdAndStatusAndEntityStatusNot(
            Long organizationId, TripStatus status, EntityStatus entityStatus, Pageable pageable);

    Optional<Trip> findFirstByFleetAssetIdAndStatusAndEntityStatusNotOrderByStartedAtDesc(
            Long fleetAssetId, TripStatus status, EntityStatus entityStatus);

    @Query("SELECT t FROM Trip t WHERE t.organizationId = :orgId " +
           "AND t.entityStatus <> :entityStatus " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:search IS NULL OR LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(t.productName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(t.fromWarehouseName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(t.toWarehouseName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.id DESC")
    Page<Trip> findByFilters(@Param("orgId") Long organizationId,
                             @Param("status") TripStatus status,
                             @Param("search") String searchTerm,
                             @Param("entityStatus") EntityStatus entityStatus,
                             Pageable pageable);

    @Query("SELECT t FROM Trip t WHERE t.organizationId = :orgId " +
           "AND t.entityStatus <> :entityStatus " +
           "AND t.status IN :statuses " +
           "AND (:search IS NULL OR :search = '' OR LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(t.productName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(t.fromWarehouseName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(t.toWarehouseName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.id DESC")
    Page<Trip> findActiveByFilters(@Param("orgId") Long organizationId,
                                 @Param("statuses") List<TripStatus> statuses,
                                 @Param("search") String searchTerm,
                                 @Param("entityStatus") EntityStatus entityStatus,
                                 Pageable pageable);

    @Query(value = """
            SELECT t.organization_id AS organizationId,
                   COUNT(*) AS activeTrips
            FROM trip t
            WHERE t.entity_status <> 'DELETED'
              AND t.status IN ('SCHEDULED','IN_TRANSIT','AT_BORDER_HOLD','ROADSIDE_HOLD',
                               'ARRIVED','COUNTING_STOCK','COUNT_COMPLETE','OTP_PENDING',
                               'RETURN_IN_TRANSIT')
            GROUP BY t.organization_id
            """, nativeQuery = true)
    List<OrganizationTripStatsProjection> aggregateActiveTripsByOrganization();

    long countByStatusInAndEntityStatusNot(List<TripStatus> statuses, EntityStatus entityStatus);

    long countByStatusAndEntityStatusNot(TripStatus status, EntityStatus entityStatus);
}
