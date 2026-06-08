package projectlx.user.management.business.logic.support;

import java.sql.Connection;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import projectlx.user.management.db.migration.LdmsRoleCatalogSeeder;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserGroup;
import projectlx.user.management.model.UserType;
import projectlx.user.management.repository.UserGroupRepository;
import projectlx.user.management.repository.UserTypeRepository;

/**
 * Idempotent workspace bootstrap: one platform-wide {@code Administrator} group (all LDMS roles)
 * and baseline user types for the organisation portal.
 */
@Component
@RequiredArgsConstructor
public class OrganizationWorkspaceProvisioner {

    private static final Logger log = LoggerFactory.getLogger(OrganizationWorkspaceProvisioner.class);
    public static final String ADMINISTRATOR_GROUP_NAME = "Administrator";
    private static final String USER_TYPE_ORGANIZATION_CONTACT = "ORGANIZATION_CONTACT";
    private static final String USER_TYPE_ORGANIZATION_MEMBER = "ORGANIZATION_MEMBER";

    private final DataSource dataSource;
    private final UserGroupRepository userGroupRepository;
    private final UserTypeRepository userTypeRepository;

    /**
     * Returns the single platform {@code Administrator} group and refreshes role grants.
     * {@code organizationId} is accepted for call-site compatibility but does not create per-org groups.
     */
    public UserGroup ensureAdministratorGroup(long organizationId) {
        syncPlatformAdministratorGroup();
        return userGroupRepository
                .findByOrganizationIdIsNullAndNameIgnoreCaseAndEntityStatusNot(
                        ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED)
                .orElse(null);
    }

    public void ensureBaselineUserTypes() {
        ensureUserType(
                OrganizationPortalUserTypePolicy.SYSTEM_ADMINISTRATOR,
                "Platform workspace administrator with full organisation access");
        ensureUserType(
                USER_TYPE_ORGANIZATION_CONTACT,
                "Primary organisation contact and workspace owner");
        ensureUserType(
                USER_TYPE_ORGANIZATION_MEMBER,
                "Standard organisation user on the platform portal");
    }

    private void syncPlatformAdministratorGroup() {
        try (Connection connection = dataSource.getConnection()) {
            LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(connection);
            log.debug("Synchronized platform Administrator group and role catalog");
        } catch (Exception ex) {
            log.error("Failed to synchronize platform Administrator group: {}", ex.getMessage(), ex);
        }
        ensureBaselineUserTypes();
    }

    private void ensureUserType(String name, String description) {
        if (userTypeRepository
                .findByUserTypeNameAndEntityStatusNot(name, EntityStatus.DELETED)
                .isPresent()) {
            return;
        }
        UserType type = new UserType();
        type.setUserTypeName(name);
        type.setDescription(description);
        userTypeRepository.save(type);
        log.info("Created user type {}", name);
    }
}
