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
}
