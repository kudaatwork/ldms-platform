package projectlx.user.management.service.repository;

import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRoleRepository extends JpaRepository<UserRole, Long>, JpaSpecificationExecutor<UserRole> {
    Optional<UserRole> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<UserRole> findByRoleAndEntityStatusNot(String role, EntityStatus entityStatus);
    List<UserRole> findByEntityStatusNot(EntityStatus entityStatus);
    Set<UserRole> findByIdInAndEntityStatusNot(List<Long> userRoleIds, EntityStatus entityStatus);
}
