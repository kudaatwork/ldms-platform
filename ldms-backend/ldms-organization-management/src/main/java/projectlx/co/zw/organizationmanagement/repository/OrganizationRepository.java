package projectlx.co.zw.organizationmanagement.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.organizationmanagement.model.Industry;
import projectlx.co.zw.organizationmanagement.model.KycStatus;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

    Optional<Organization> findByEmailAndEntityStatusNot(String email, EntityStatus deleted);
    Optional<Organization> findByEmail(String email);

    Optional<Organization> findByIdAndEntityStatusNot(Long id, EntityStatus deleted);

    long countByIndustryAndEntityStatusNot(Industry industry, EntityStatus deleted);

    long countByIndustryAndIsVerifiedTrueAndEntityStatusNot(Industry industry, EntityStatus deleted);

    List<Organization> findByIndustryAndEntityStatusNotOrderByNameAsc(Industry industry, EntityStatus deleted, Pageable pageable);

    long countByAssignedStage1ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
            Long assignedStage1ApproverUserId,
            Collection<KycStatus> kycStatuses,
            EntityStatus entityStatus);

    long countByAssignedStage2ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
            Long assignedStage2ApproverUserId,
            Collection<KycStatus> kycStatuses,
            EntityStatus entityStatus);

    long countByAssignedStage3ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
            Long assignedStage3ApproverUserId,
            Collection<KycStatus> kycStatuses,
            EntityStatus entityStatus);

    long countByAssignedStage4ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
            Long assignedStage4ApproverUserId,
            Collection<KycStatus> kycStatuses,
            EntityStatus entityStatus);

    long countByAssignedStage5ApproverUserIdAndCreatedViaSignupTrueAndKycStatusInAndEntityStatusNot(
            Long assignedStage5ApproverUserId,
            Collection<KycStatus> kycStatuses,
            EntityStatus entityStatus);

    @Query("""
            SELECT link.transporter FROM ContractedTransporterLink link
            WHERE link.organization.id = :supplierId
            AND link.entityStatus <> :deleted
            AND link.transporter.entityStatus <> :deleted
            ORDER BY link.transporter.name ASC
            """)
    List<Organization> findContractedTransportersForSupplier(
            @Param("supplierId") Long supplierId,
            @Param("deleted") EntityStatus deleted);

    @Query("""
            SELECT link.transporter FROM ContractedTransporterLink link
            WHERE link.organization.id = :supplierId
            AND link.linkStatus = projectlx.co.zw.organizationmanagement.utils.enums.TransporterLinkStatus.ACCEPTED
            AND link.entityStatus <> :deleted
            AND link.transporter.entityStatus <> :deleted
            ORDER BY link.transporter.name ASC
            """)
    List<Organization> findAcceptedContractedTransportersForSupplier(
            @Param("supplierId") Long supplierId,
            @Param("deleted") EntityStatus deleted);

    @Query("""
            SELECT link.organization FROM ContractedTransporterLink link
            WHERE link.transporter.id = :transporterId
            AND link.entityStatus <> :deleted
            AND link.organization.entityStatus <> :deleted
            ORDER BY link.organization.name ASC
            """)
    List<Organization> findContractingOrganizationsForTransporter(
            @Param("transporterId") Long transporterId,
            @Param("deleted") EntityStatus deleted);
}
