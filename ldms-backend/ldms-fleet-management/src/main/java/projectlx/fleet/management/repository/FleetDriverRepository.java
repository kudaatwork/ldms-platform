package projectlx.fleet.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.fleet.management.model.FleetDriver;
import projectlx.fleet.management.utils.enums.DriverEmploymentType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Collection;
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

    /** Finds the driver profile linked to the given platform user id. */
    Optional<FleetDriver> findByUserIdAndEntityStatusNot(Long userId, EntityStatus entityStatus);

    /** Marketplace: active, visible drivers. */
    @Query("SELECT d FROM FleetDriver d WHERE d.marketplaceVisible = true AND d.entityStatus <> :excluded")
    List<FleetDriver> findMarketplaceDrivers(@Param("excluded") EntityStatus excluded);

    /**
     * Marketplace search with optional filters.
     * Returns visible drivers NOT already employed by {@code excludeOrganizationId}.
     */
    @Query("""
            SELECT d FROM FleetDriver d
            WHERE d.marketplaceVisible = true
              AND d.entityStatus <> :excluded
              AND d.organizationId <> :excludeOrgId
              AND (:term IS NULL OR LOWER(d.firstName) LIKE LOWER(CONCAT('%',:term,'%'))
                                 OR LOWER(d.lastName) LIKE LOWER(CONCAT('%',:term,'%'))
                                 OR LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%',:term,'%')))
              AND (:licenseClass IS NULL OR LOWER(d.licenseClass) = LOWER(:licenseClass))
            ORDER BY d.id DESC
            """)
    List<FleetDriver> searchMarketplace(
            @Param("excluded") EntityStatus excluded,
            @Param("excludeOrgId") Long excludeOrgId,
            @Param("term") String term,
            @Param("licenseClass") String licenseClass);

    /** Checks if the caller org already has a driver linked to the given userId. */
    boolean existsByUserIdAndOrganizationIdAndEntityStatusNot(Long userId, Long organizationId, EntityStatus entityStatus);

    long countByOrganizationIdAndEntityStatusNot(Long organizationId, EntityStatus entityStatus);

    long countByEntityStatusNot(EntityStatus entityStatus);

    List<FleetDriver> findByOrganizationIdAndEmploymentTypeAndEntityStatusNotOrderByIdDesc(
            Long organizationId, DriverEmploymentType employmentType, EntityStatus entityStatus);

    List<FleetDriver> findByOrganizationIdInAndEntityStatusNotOrderByIdDesc(
            Collection<Long> organizationIds, EntityStatus entityStatus);
}
