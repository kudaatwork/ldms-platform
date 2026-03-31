package projectlx.user.management.service.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface UserSecurityRepository extends JpaRepository<UserSecurity, Long>, JpaSpecificationExecutor<UserSecurity> {
    Optional<UserSecurity> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    List<UserSecurity> findByEntityStatusNot(EntityStatus entityStatus);
    @Query("SELECT COUNT(u) FROM UserSecurity u WHERE u.securityQuestion_1 = :q1 AND u.securityAnswer_1 = :a1 AND " +
            "u.securityQuestion_2 = :q2 AND u.securityAnswer_2 = :a2 AND u.entityStatus <> :status")
    long countMatchingSecuritySetup(@Param("q1") String securityQuestion1,
                                    @Param("a1") String securityAnswer1,
                                    @Param("q2") String securityQuestion2,
                                    @Param("a2") String securityAnswer2,
                                    @Param("status") EntityStatus entityStatus);
}
