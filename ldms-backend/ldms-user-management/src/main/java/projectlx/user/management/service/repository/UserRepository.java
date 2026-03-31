package projectlx.user.management.service.repository;

import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<User> findByPhoneNumberAndEntityStatusNot(String phoneNumber, EntityStatus entityStatus);
    List<User> findByEntityStatusNot(EntityStatus entityStatus);
    Optional<User> findByUsernameAndEntityStatusNot(String username, EntityStatus entityStatus);
    Optional<User> findByEmailAndEntityStatusNot(String email, EntityStatus entityStatus);
    List<User> findByOrganizationIdAndEntityStatusNot(Long organizationId, EntityStatus entityStatus);
    List<User> findByBranchIdAndEntityStatusNot(Long branchId, EntityStatus entityStatus);
    List<User> findByAgentIdAndEntityStatusNot(Long agentId, EntityStatus entityStatus);
}
