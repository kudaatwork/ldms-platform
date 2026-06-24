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

/**
 * Idempotent workspace bootstrap: platform role catalog plus one {@code Administrator} group per
 * organisation workspace (scoped by {@code organization_id}).
 */
@Component
@RequiredArgsConstructor
public class OrganizationWorkspaceProvisioner {

    private static final Logger log = LoggerFactory.getLogger(OrganizationWorkspaceProvisioner.class);
    public static final String ADMINISTRATOR_GROUP_NAME = "Administrator";
    private static final String USER_TYPE_ORGANIZATION_CONTACT = "ORGANIZATION_CONTACT";
    private static final String USER_TYPE_ORGANIZATION_MEMBER = "ORGANIZATION_MEMBER";
    private static final String USER_TYPE_FLEET_DRIVER = "FLEET_DRIVER";

    private final DataSource dataSource;
    private final UserGroupRepository userGroupRepository;
    private final UserTypeRepository userTypeRepository;

    /**
     * Ensures the organisation-scoped {@code Administrator} group exists with workspace portal roles.
     */
    public UserGroup ensureAdministratorGroup(long organizationId) {
        return ensureAdministratorGroup(organizationId, null);
    }

    public UserGroup ensureAdministratorGroup(long organizationId, String organizationClassification) {
        syncRoleCatalog();
        if (organizationId <= 0) {
            return userGroupRepository
                    .findByOrganizationIdIsNullAndNameIgnoreCaseAndEntityStatusNot(
                            ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED)
                    .orElse(null);
        }
        // Check if an active Administrator group already exists for this organisation
        Optional<UserGroup> existing = userGroupRepository
                .findByOrganizationIdAndNameIgnoreCaseAndEntityStatusNot(
                        organizationId, ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED);
        if (existing.isPresent()) {
            UserGroup group = existing.get();
            if (!group.isSystemGroup()) {
                group.setSystemGroup(true);
                userGroupRepository.save(group);
            }
            if (organizationClassification != null && !organizationClassification.isBlank()) {
                group.setOrganizationClassification(organizationClassification.trim().toUpperCase());
                userGroupRepository.save(group);
            }
            return group;
        }
        try (Connection connection = dataSource.getConnection()) {
            String classification = organizationClassification != null ? organizationClassification.trim().toUpperCase() : null;
            long groupId = LdmsRoleCatalogSeeder.seedOrganizationAdministratorGroup(connection, organizationId, classification);
            UserGroup group = userGroupRepository.findById(groupId).orElse(null);
            if (group != null) {
                group.setSystemGroup(true);
                if (organizationClassification != null && !organizationClassification.isBlank()) {
                    group.setOrganizationClassification(organizationClassification.trim().toUpperCase());
                }
                userGroupRepository.save(group);
            }
            return group;
        } catch (Exception ex) {
            log.error("Failed to ensure organisation Administrator group for org {}: {}",
                    organizationId, ex.getMessage(), ex);
            return userGroupRepository
                    .findByOrganizationIdAndNameIgnoreCaseAndEntityStatusNot(
                            organizationId, ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED)
                    .orElse(null);
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
