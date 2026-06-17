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
}
