package projectlx.user.management.repository;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRoleRepository extends JpaRepository<UserRole, Long>, JpaSpecificationExecutor<UserRole> {

    @Query("SELECT COUNT(ur) FROM UserRole ur JOIN ur.userGroups ug "
            + "WHERE ug.id = :userGroupId AND ur.entityStatus <> :excluded")
    long countActiveRolesForUserGroup(
            @Param("userGroupId") Long userGroupId,
            @Param("excluded") EntityStatus excluded);

    Optional<UserRole> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<UserRole> findByRoleAndEntityStatusNot(String role, EntityStatus entityStatus);
    Optional<UserRole> findByRole(String role);
    List<UserRole> findByEntityStatusNot(EntityStatus entityStatus);
    Set<UserRole> findByIdInAndEntityStatusNot(List<Long> userRoleIds, EntityStatus entityStatus);
}
