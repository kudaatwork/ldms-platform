package projectlx.shipment.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.shipment.management.model.Shipment;
import projectlx.shipment.management.repository.projection.DailyShipmentVolumeProjection;
import projectlx.shipment.management.repository.projection.OrganizationShipmentStatsProjection;
import projectlx.shipment.management.repository.projection.ShipmentStatusCountProjection;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long>,
        JpaSpecificationExecutor<Shipment>, RepositoryMarkerInterface {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Shipment> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    Optional<Shipment> findByIdAndOrganizationIdAndEntityStatusNot(Long id, Long organizationId, EntityStatus entityStatus);

    Optional<Shipment> findByInventoryTransferIdAndEntityStatusNot(Long inventoryTransferId, EntityStatus entityStatus);

    Optional<Shipment> findBySalesOrderIdAndEntityStatusNot(Long salesOrderId, EntityStatus entityStatus);

    List<Shipment> findByOrganizationIdAndEntityStatusNotOrderByIdDesc(Long organizationId, EntityStatus entityStatus);

    List<Shipment> findByTransportCompanyOrganizationIdAndEntityStatusNotOrderByIdDesc(
            Long transportCompanyOrganizationId, EntityStatus entityStatus);

    List<Shipment> findByOrganizationIdAndStatusAndTransportCompanyOrganizationIdAndEntityStatusNotOrderByIdAsc(
            Long organizationId,
            projectlx.shipment.management.utils.enums.ShipmentStatus status,
            Long transportCompanyOrganizationId,
            EntityStatus entityStatus);

    boolean existsByInventoryTransferIdAndEntityStatusNot(Long inventoryTransferId, EntityStatus entityStatus);

    boolean existsBySalesOrderIdAndEntityStatusNot(Long salesOrderId, EntityStatus entityStatus);

    boolean existsByCrossDockDispatchIdAndEntityStatusNot(Long crossDockDispatchId, EntityStatus entityStatus);

    @Query(value = """
            SELECT s.organization_id AS organizationId,
                   SUM(CASE WHEN s.status IN ('PENDING_ALLOCATION','PENDING_FLEET_ALLOCATION','ALLOCATED','IN_TRANSIT','ARRIVED_PENDING_OTP')
                       THEN 1 ELSE 0 END) AS activeShipments,
                   SUM(CASE WHEN s.status = 'DELIVERED' AND s.modified_at >= :monthStart THEN 1 ELSE 0 END) AS completedThisMonth,
                   MAX(s.modified_at) AS lastActivityAt
            FROM shipment s
            WHERE s.entity_status <> 'DELETED'
            GROUP BY s.organization_id
            """, nativeQuery = true)
    List<OrganizationShipmentStatsProjection> aggregateStatsByOwnerOrganization(
            @Param("monthStart") LocalDateTime monthStart);

    @Query(value = """
            SELECT s.transport_company_organization_id AS organizationId,
                   SUM(CASE WHEN s.status IN ('PENDING_ALLOCATION','PENDING_FLEET_ALLOCATION','ALLOCATED','IN_TRANSIT','ARRIVED_PENDING_OTP')
                       THEN 1 ELSE 0 END) AS activeShipments,
                   SUM(CASE WHEN s.status = 'DELIVERED' AND s.modified_at >= :monthStart THEN 1 ELSE 0 END) AS completedThisMonth,
                   MAX(s.modified_at) AS lastActivityAt
            FROM shipment s
            WHERE s.entity_status <> 'DELETED'
              AND s.transport_company_organization_id IS NOT NULL
            GROUP BY s.transport_company_organization_id
            """, nativeQuery = true)
    List<OrganizationShipmentStatsProjection> aggregateStatsByTransportOrganization(
            @Param("monthStart") LocalDateTime monthStart);

    @Query(value = """
            SELECT s.status AS status, COUNT(*) AS count
            FROM shipment s
            WHERE s.entity_status <> 'DELETED'
            GROUP BY s.status
            """, nativeQuery = true)
    List<ShipmentStatusCountProjection> countByStatus();

    @Query(value = """
            SELECT DATE(s.created_at) AS day, COUNT(*) AS count
            FROM shipment s
            WHERE s.entity_status <> 'DELETED'
              AND s.created_at >= :weekStart
            GROUP BY DATE(s.created_at)
            ORDER BY day
            """, nativeQuery = true)
    List<DailyShipmentVolumeProjection> countCreatedSince(@Param("weekStart") LocalDateTime weekStart);

    List<Shipment> findTop20ByStatusInAndEntityStatusNotOrderByModifiedAtDesc(
            List<projectlx.shipment.management.utils.enums.ShipmentStatus> statuses,
            EntityStatus entityStatus);

    long countByStatusInAndEntityStatusNot(
            List<projectlx.shipment.management.utils.enums.ShipmentStatus> statuses,
            EntityStatus entityStatus);

    long countByStatusAndModifiedAtGreaterThanEqualAndEntityStatusNot(
            projectlx.shipment.management.utils.enums.ShipmentStatus status,
            LocalDateTime modifiedAt,
            EntityStatus entityStatus);
}
