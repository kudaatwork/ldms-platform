package projectlx.inventory.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.inventory.management.model.PurchaseRequisition;
import projectlx.inventory.management.model.PurchaseRequisitionStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for Purchase Requisition entity.
 * Provides CRUD operations and custom queries for PR management.
 */
public interface PurchaseRequisitionRepository extends
        JpaRepository<PurchaseRequisition, Long>,
        JpaSpecificationExecutor<PurchaseRequisition> {

    /**
     * Find PR by ID with pessimistic write lock (for updates)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseRequisition> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    /**
     * Find PR by requisition number
     */
    Optional<PurchaseRequisition> findByRequisitionNumberAndEntityStatusNot(
            String requisitionNumber,
            EntityStatus entityStatus
    );

    /**
     * Check if requisition number exists
     */
    boolean existsByRequisitionNumber(String requisitionNumber);

    /**
     * True when any non-deleted purchase requisition references the department.
     */
    boolean existsByDepartmentIdAndEntityStatusNot(Long departmentId, EntityStatus entityStatus);

    /**
     * Find all PRs by requester
     */
    List<PurchaseRequisition> findByRequestedByUserIdAndEntityStatusNot(
            Long requestedByUserId,
            EntityStatus entityStatus
    );

    /**
     * Find all PRs by department and status
     */
    List<PurchaseRequisition> findByDepartmentIdAndStatusAndEntityStatusNot(
            Long departmentId,
            PurchaseRequisitionStatus status,
            EntityStatus entityStatus
    );

    /**
     * Find all PRs by organization and status
     */
    List<PurchaseRequisition> findByOrganizationIdAndStatusAndEntityStatusNot(
            Long organizationId,
            PurchaseRequisitionStatus status,
            EntityStatus entityStatus
    );

    /**
     * Find all PRs pending approval for department
     */
    @Query("SELECT pr FROM PurchaseRequisition pr " +
           "WHERE pr.departmentId = :departmentId " +
           "AND pr.status = 'SUBMITTED' " +
           "AND pr.entityStatus != :entityStatus " +
           "ORDER BY pr.priority DESC, pr.createdAt ASC")
    List<PurchaseRequisition> findPendingApprovalsByDepartment(
            @Param("departmentId") Long departmentId,
            @Param("entityStatus") EntityStatus entityStatus
    );

    /**
     * Find all approved PRs ready for fulfillment
     */
    @Query("SELECT pr FROM PurchaseRequisition pr " +
           "WHERE pr.organizationId = :organizationId " +
           "AND pr.status IN ('CUSTOMER_ACKNOWLEDGED', 'PARTIALLY_FULFILLED') " +
           "AND pr.entityStatus != :entityStatus " +
           "ORDER BY pr.priority DESC, pr.requiredByDate ASC")
    List<PurchaseRequisition> findApprovedPendingFulfillment(
            @Param("organizationId") Long organizationId,
            @Param("entityStatus") EntityStatus entityStatus
    );

    /**
     * Find PRs expiring soon (within next N days)
     */
    @Query("SELECT pr FROM PurchaseRequisition pr " +
           "WHERE pr.expiryDate IS NOT NULL " +
           "AND pr.expiryDate BETWEEN :startDate AND :endDate " +
           "AND pr.status NOT IN ('FULFILLED', 'CLOSED', 'CANCELLED', 'EXPIRED') " +
           "AND pr.entityStatus != :entityStatus")
    List<PurchaseRequisition> findExpiringSoon(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("entityStatus") EntityStatus entityStatus
    );

    /**
     * Find expired PRs that need status update
     */
    @Query("SELECT pr FROM PurchaseRequisition pr " +
           "WHERE pr.expiryDate < :currentDate " +
           "AND pr.status NOT IN ('FULFILLED', 'CLOSED', 'CANCELLED', 'EXPIRED') " +
           "AND pr.entityStatus != :entityStatus")
    List<PurchaseRequisition> findExpired(
            @Param("currentDate") LocalDate currentDate,
            @Param("entityStatus") EntityStatus entityStatus
    );

    /**
     * Count PRs by status for dashboard
     */
    @Query("SELECT pr.status, COUNT(pr) FROM PurchaseRequisition pr " +
           "WHERE pr.organizationId = :organizationId " +
           "AND pr.entityStatus != :entityStatus " +
           "GROUP BY pr.status")
    List<Object[]> countByStatusForOrganization(
            @Param("organizationId") Long organizationId,
            @Param("entityStatus") EntityStatus entityStatus
    );

    /**
     * Find PRs visible to a specific supplier (published to them)
     */
    List<PurchaseRequisition> findByPreferredSupplierIdAndStatusAndEntityStatusNot(
            Long preferredSupplierId,
            PurchaseRequisitionStatus status,
            EntityStatus entityStatus
    );

    /**
     * Find all PRs in SUBMITTED status for a specific organisation (org-scoped pending approvals)
     */
    @Query("SELECT pr FROM PurchaseRequisition pr " +
           "WHERE pr.organizationId = :organizationId " +
           "AND pr.status = 'SUBMITTED' " +
           "AND pr.entityStatus != :entityStatus " +
           "ORDER BY pr.priority DESC, pr.createdAt ASC")
    List<PurchaseRequisition> findPendingApprovalsByOrganization(
            @Param("organizationId") Long organizationId,
            @Param("entityStatus") EntityStatus entityStatus
    );

    @Query("SELECT DISTINCT pr.departmentId FROM PurchaseRequisition pr "
            + "WHERE pr.departmentId IN :departmentIds AND pr.entityStatus != :entityStatus")
    Set<Long> findDepartmentIdsReferencedByRequisitions(
            @Param("departmentIds") Collection<Long> departmentIds,
            @Param("entityStatus") EntityStatus entityStatus);
}
