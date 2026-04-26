package projectlx.user.management.repository;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserPasswordRepository extends JpaRepository<UserPassword, Long>, JpaSpecificationExecutor<UserPassword> {
    Optional<UserPassword> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<UserPassword> findByPasswordAndEntityStatusNot(String password, EntityStatus entityStatus);
    List<UserPassword> findByEntityStatusNot(EntityStatus entityStatus);
    Optional<UserPassword> findByUserIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    
    /**
     * Find passwords that are about to expire within the specified date range
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @param entityStatus The entity status to exclude
     * @return List of passwords that are about to expire
     */
    @Query("SELECT up FROM UserPassword up WHERE up.expiryDate BETWEEN :startDate AND :endDate AND up.isPasswordExpired = false AND up.entityStatus != :entityStatus")
    List<UserPassword> findPasswordsAboutToExpire(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("entityStatus") EntityStatus entityStatus);
}
