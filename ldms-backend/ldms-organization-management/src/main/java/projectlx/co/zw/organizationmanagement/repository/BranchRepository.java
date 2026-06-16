package projectlx.co.zw.organizationmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {

    List<Branch> findByOrganizationAndEntityStatusNot(Organization organization, EntityStatus deleted);

    Optional<Branch> findByIdAndEntityStatusNot(Long id, EntityStatus deleted);

    java.util.List<Branch> findByParentBranchIdAndEntityStatusNot(Long parentBranchId, EntityStatus deleted);

    @org.springframework.data.jpa.repository.Query("""
            SELECT b FROM Branch b
            WHERE b.organization.id = :organizationId
              AND b.isHeadOffice = TRUE
              AND b.entityStatus <> 'DELETED'
            """)
    Optional<Branch> findHeadOfficeByOrganizationId(
            @org.springframework.data.repository.query.Param("organizationId") Long organizationId);
}
