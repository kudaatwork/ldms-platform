package projectlx.user.management.service.repository;

import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface UserTypeRepository extends JpaRepository<UserType, Long>, JpaSpecificationExecutor<UserType> {
    Optional<UserType> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<UserType> findByUserTypeNameAndEntityStatusNot(String name, EntityStatus entityStatus);
    List<UserType> findByEntityStatusNot(EntityStatus entityStatus);
}
