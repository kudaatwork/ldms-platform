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
 * Idempotent workspace bootstrap for a new organisation: scoped {@code Administrator} group with
 * organisation-portal permissions and baseline user types used by the platform portal.
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

    public UserGroup ensureAdministratorGroup(long organizationId) {
        if (organizationId <= 0) {
            return null;
        }
        return userGroupRepository
                .findByOrganizationIdAndNameIgnoreCaseAndEntityStatusNot(
                        organizationId, ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED)
                .orElseGet(() -> loadAfterSync(organizationId));
    }

    public void ensureBaselineUserTypes() {
        ensureUserType(
                USER_TYPE_ORGANIZATION_CONTACT,
                "Primary organisation contact and workspace owner");
        ensureUserType(
                USER_TYPE_ORGANIZATION_MEMBER,
                "Standard organisation user on the platform portal");
    }

    private UserGroup loadAfterSync(long organizationId) {
        syncOrganizationWorkspace(organizationId);
        return userGroupRepository
                .findByOrganizationIdAndNameIgnoreCaseAndEntityStatusNot(
                        organizationId, ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED)
                .orElse(null);
    }

    private void syncOrganizationWorkspace(long organizationId) {
        try (Connection connection = dataSource.getConnection()) {
            LdmsRoleCatalogSeeder.seedOrganizationAdministratorGroup(connection, organizationId);
            log.info("Synchronized organisation {} workspace Administrator group and roles", organizationId);
        } catch (Exception ex) {
            log.error(
                    "Failed to synchronize organisation {} workspace: {}",
                    organizationId,
                    ex.getMessage(),
                    ex);
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
