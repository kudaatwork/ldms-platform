package projectlx.fleet.management.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.fleet.management.model.FleetAsset;
import projectlx.fleet.management.utils.enums.FleetOwnershipType;
import projectlx.fleet.management.utils.enums.FleetRegistrationStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface FleetAssetRepository extends JpaRepository<FleetAsset, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FleetAsset> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<FleetAsset> findByOrganizationIdAndEntityStatusNotOrderByIdDesc(Long organizationId, EntityStatus entityStatus);

    List<FleetAsset> findByOrganizationIdAndRegistrationStatusAndEntityStatusNotOrderByIdDesc(
            Long organizationId, FleetRegistrationStatus registrationStatus, EntityStatus entityStatus);

    Optional<FleetAsset> findByIdAndOrganizationIdAndEntityStatusNot(Long id, Long organizationId, EntityStatus entityStatus);

    long countByOrganizationIdAndOwnershipTypeAndEntityStatusNot(
            Long organizationId, FleetOwnershipType ownershipType, EntityStatus entityStatus);

    long countByOwnershipTypeAndEntityStatusNot(FleetOwnershipType ownershipType, EntityStatus entityStatus);

    long countByEntityStatusNot(EntityStatus entityStatus);

    @Query("""
            SELECT COUNT(DISTINCT a.organizationId) FROM FleetAsset a
            WHERE a.entityStatus <> :excluded
            """)
    long countDistinctOrganizationIdByEntityStatusNot(@Param("excluded") EntityStatus excluded);

    @Query("""
            SELECT DISTINCT a.contractedTransporterOrganizationId FROM FleetAsset a
            WHERE a.organizationId = :organizationId
              AND a.ownershipType = :ownershipType
              AND a.contractedTransporterOrganizationId IS NOT NULL
              AND a.entityStatus <> :excluded
            """)
    List<Long> findDistinctContractedTransporterOrganizationIds(
            @Param("organizationId") Long organizationId,
            @Param("ownershipType") FleetOwnershipType ownershipType,
            @Param("excluded") EntityStatus excluded);
}
