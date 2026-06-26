package projectlx.user.management.repository;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long>, JpaSpecificationExecutor<UserGroup> {
    Optional<UserGroup> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<UserGroup> findByNameAndEntityStatusNot(String name, EntityStatus entityStatus);
    Optional<UserGroup> findByNameIgnoreCaseAndEntityStatusNot(String name, EntityStatus entityStatus);
    Optional<UserGroup> findByOrganizationIdIsNullAndNameIgnoreCaseAndEntityStatusNot(
            String name, EntityStatus entityStatus);
    /** The shared classification default admin group (organisation_id IS NULL + classification). */
    Optional<UserGroup> findByOrganizationIdIsNullAndOrganizationClassificationIgnoreCaseAndNameIgnoreCaseAndEntityStatusNot(
            String organizationClassification, String name, EntityStatus entityStatus);
    Optional<UserGroup> findByOrganizationIdAndNameIgnoreCaseAndEntityStatusNot(
            Long organizationId, String name, EntityStatus entityStatus);
    Optional<UserGroup> findByName(String name);
    List<UserGroup> findByEntityStatusNot(EntityStatus entityStatus);

    /** Organisation-scoped groups (e.g. Administrator) for a given organisation classification. */
    List<UserGroup> findByOrganizationClassificationIgnoreCaseAndEntityStatusNot(
            String organizationClassification, EntityStatus entityStatus);

    /** Count active users in a given user group. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(u) FROM User u WHERE u.userGroup.id = :groupId AND u.entityStatus <> :excluded")
    long countActiveUsersInGroup(@org.springframework.data.repository.query.Param("groupId") Long groupId,
                                  @org.springframework.data.repository.query.Param("excluded") EntityStatus excluded);
}
