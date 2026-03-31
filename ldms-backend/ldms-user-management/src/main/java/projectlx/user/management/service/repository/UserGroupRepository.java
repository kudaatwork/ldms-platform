package projectlx.user.management.service.repository;

import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long>, JpaSpecificationExecutor<UserGroup> {
    Optional<UserGroup> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<UserGroup> findByNameAndEntityStatusNot(String name, EntityStatus entityStatus);
    List<UserGroup> findByEntityStatusNot(EntityStatus entityStatus);
}
