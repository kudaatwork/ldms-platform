package projectlx.user.management.repository;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query("SELECT COUNT(u) FROM User u WHERE u.userGroup IS NOT NULL AND u.userGroup.id = :userGroupId "
            + "AND u.entityStatus <> :excluded")
    long countActiveUsersForUserGroup(
            @Param("userGroupId") Long userGroupId,
            @Param("excluded") EntityStatus excluded);

    Optional<User> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<User> findByUserGroup_IdAndEntityStatusNot(Long userGroupId, EntityStatus entityStatus);
    Optional<User> findByPhoneNumberAndEntityStatusNot(String phoneNumber, EntityStatus entityStatus);
    List<User> findByEntityStatusNot(EntityStatus entityStatus);
    Optional<User> findByUsernameAndEntityStatusNot(String username, EntityStatus entityStatus);
    Optional<User> findByEmailAndEntityStatusNot(String email, EntityStatus entityStatus);
    List<User> findByOrganizationIdAndEntityStatusNot(Long organizationId, EntityStatus entityStatus);
    List<User> findByBranchIdAndEntityStatusNot(Long branchId, EntityStatus entityStatus);

    List<User> findByOrganizationKycApproverTrueAndOrganizationIdIsNullAndEntityStatusNot(
            EntityStatus entityStatus);

    /**
     * Returns {@code min( actual non-deleted user count for user type , 2 )}. Used so we only need to know {@code >= 2}
     * for branching, without scanning entire {@code user}.
     */
    @Query(
            value = "SELECT COUNT(*) FROM (SELECT id FROM `user` WHERE user_type_id = :userTypeId "
                    + "AND entity_status <> :excludedStatus LIMIT 2) capped_rows",
            nativeQuery = true)
    long countNonDeletedUsersForUserTypeAtMostTwo(
            @Param("userTypeId") Long userTypeId,
            @Param("excludedStatus") String excludedEntityStatusName);
}
