package projectlx.user.management.business.logic.support;

import java.sql.Connection;
import java.util.Optional;
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
import projectlx.user.management.utils.support.UserGroupNameSupport;

/**
 * Idempotent workspace bootstrap: platform role catalog plus one {@code Administrator} group per
 * organisation workspace (scoped by {@code organization_id}).
 */
@Component
@RequiredArgsConstructor
public class OrganizationWorkspaceProvisioner {

    private static final Logger log = LoggerFactory.getLogger(OrganizationWorkspaceProvisioner.class);
    public static final String ADMINISTRATOR_GROUP_NAME = UserGroupNameSupport.ADMINISTRATOR_GROUP_NAME;
    private static final String ADMIN_PORTAL_CLASSIFICATION = "ADMIN_PORTAL";
    private static final String USER_TYPE_ORGANIZATION_CONTACT = "ORGANIZATION_CONTACT";
    private static final String USER_TYPE_ORGANIZATION_MEMBER = "ORGANIZATION_MEMBER";
    private static final String USER_TYPE_FLEET_DRIVER = "FLEET_DRIVER";

    private final DataSource dataSource;
    private final UserGroupRepository userGroupRepository;
    private final UserTypeRepository userTypeRepository;

    /**
     * Returns the platform-wide {@code Administrator} group (organisation_id IS NULL, ADMIN_PORTAL).
     */
    public UserGroup ensurePlatformAdministratorGroup() {
        syncRoleCatalog();
        return userGroupRepository
                .findByOrganizationIdIsNullAndOrganizationClassificationIgnoreCaseAndNameIgnoreCaseAndEntityStatusNot(
                        ADMIN_PORTAL_CLASSIFICATION, ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED)
                .orElseGet(() -> userGroupRepository
                        .findByOrganizationIdIsNullAndNameIgnoreCaseAndEntityStatusNot(
                                ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED)
                        .orElse(null));
    }

    /**
     * Ensures the shared classification default {@code Administrator} group exists (organisation_id IS
     * NULL, one per classification) and returns it. This is the single admin group that admin users of
     * every organisation in the classification inherit their roles from. Per-organisation Administrator
     * groups are no longer created.
     */
    public UserGroup ensureClassificationDefaultAdminGroup(String organizationClassification) {
        syncRoleCatalog();
        if (organizationClassification == null || organizationClassification.isBlank()) {
            return null;
        }
        String classification = organizationClassification.trim().toUpperCase();
        try (Connection connection = dataSource.getConnection()) {
            long groupId = LdmsRoleCatalogSeeder.seedClassificationDefaultAdminGroup(connection, classification);
            return userGroupRepository.findById(groupId).orElse(null);
        } catch (Exception ex) {
            log.error("Failed to ensure classification default admin group for {}: {}",
                    classification, ex.getMessage(), ex);
            return null;
        }
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
        ensureUserType(
                USER_TYPE_FLEET_DRIVER,
                "Fleet driver with mobile app access");
    }

    private void syncRoleCatalog() {
        try (Connection connection = dataSource.getConnection()) {
            LdmsRoleCatalogSeeder.seedAllRolesAndAdministratorGroup(connection);
            log.debug("Synchronized LDMS role catalog and platform Administrator group");
        } catch (Exception ex) {
            log.error("Failed to synchronize role catalog: {}", ex.getMessage(), ex);
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
