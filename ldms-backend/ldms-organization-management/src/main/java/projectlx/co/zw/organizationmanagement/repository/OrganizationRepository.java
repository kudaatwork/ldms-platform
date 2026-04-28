package projectlx.co.zw.organizationmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

    Optional<Organization> findByEmailAndEntityStatusNot(String email, EntityStatus deleted);
    Optional<Organization> findByEmail(String email);

    Optional<Organization> findByIdAndEntityStatusNot(Long id, EntityStatus deleted);
}
