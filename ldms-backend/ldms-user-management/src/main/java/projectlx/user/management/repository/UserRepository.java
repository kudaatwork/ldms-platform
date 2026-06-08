package projectlx.user.management.repository;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username) AND u.entityStatus <> :excluded")
    Optional<User> findByUsernameIgnoreCaseAndEntityStatusNot(
            @Param("username") String username,
            @Param("excluded") EntityStatus excluded);

    @EntityGraph(attributePaths = {
            "userGroup", "userGroup.userRoles", "userAccount", "userType", "userPassword", "userSecurity"
    })
    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username) AND u.entityStatus <> :excluded")
    Optional<User> findSessionProfileByUsernameIgnoreCaseAndEntityStatusNot(
            @Param("username") String username,
            @Param("excluded") EntityStatus excluded);

    @EntityGraph(attributePaths = {
            "userGroup", "userGroup.userRoles", "userAccount", "userType", "userPassword", "userSecurity"
    })
    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber AND u.entityStatus <> :excluded")
    Optional<User> findSessionProfileByPhoneNumberAndEntityStatusNot(
            @Param("phoneNumber") String phoneNumber,
            @Param("excluded") EntityStatus excluded);

    @EntityGraph(attributePaths = {
            "userGroup", "userGroup.userRoles", "userAccount", "userType", "userPassword", "userSecurity"
    })
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.entityStatus <> :excluded")
    Optional<User> findSessionProfileByEmailIgnoreCaseAndEntityStatusNot(
            @Param("email") String email,
            @Param("excluded") EntityStatus excluded);

    Optional<User> findByEmailAndEntityStatusNot(String email, EntityStatus entityStatus);
    List<User> findByOrganizationIdAndEntityStatusNot(Long organizationId, EntityStatus entityStatus);

    /** Organisation workspace listing: {@code user.organization_id} or linked {@code user_group.organization_id}. */
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN u.userGroup g
            WHERE u.entityStatus <> :excluded
              AND (u.organizationId = :organizationId OR g.organizationId = :organizationId)
            """)
    List<User> findByOrganizationWorkspace(
            @Param("organizationId") Long organizationId,
            @Param("excluded") EntityStatus excluded);

    /** Login names only — used by organisation audit views (avoids full {@link User} hydration). */
    @Query("""
            SELECT DISTINCT u.username FROM User u
            LEFT JOIN u.userGroup g
            WHERE u.entityStatus <> :excluded
              AND u.username IS NOT NULL AND TRIM(u.username) <> ''
              AND (u.organizationId = :organizationId OR g.organizationId = :organizationId)
            """)
    List<String> findUsernamesByOrganizationWorkspace(
            @Param("organizationId") Long organizationId,
            @Param("excluded") EntityStatus excluded);

    List<User> findByBranchIdAndEntityStatusNot(Long branchId, EntityStatus entityStatus);

    List<User> findByOrganizationKycApproverTrueAndOrganizationIdIsNullAndEntityStatusNot(
            EntityStatus entityStatus);

    List<User> findByOperationalIssueHandlerTrueAndOrganizationIdIsNullAndEntityStatusNot(
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
