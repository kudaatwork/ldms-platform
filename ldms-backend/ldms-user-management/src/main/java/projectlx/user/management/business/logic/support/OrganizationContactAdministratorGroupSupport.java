package projectlx.user.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserGroup;

/**
 * Links organisation contact users to the platform-wide {@code Administrator} user group.
 */
@Component
@RequiredArgsConstructor
public class OrganizationContactAdministratorGroupSupport {

    private static final Logger log = LoggerFactory.getLogger(OrganizationContactAdministratorGroupSupport.class);

    private final OrganizationWorkspaceProvisioner organizationWorkspaceProvisioner;

    public void assignIfPresent(User user) {
        assignIfPresent(user, null);
    }

    public void assignIfPresent(User user, String organizationClassification) {
        if (user == null || user.getId() == null) {
            return;
        }
        Long organizationId = user.getOrganizationId();
        if (organizationId == null || organizationId <= 0) {
            log.warn(
                    "Contact user {} has no organisation id; skipping Administrator group assignment",
                    user.getId());
            return;
        }
        organizationWorkspaceProvisioner.ensureBaselineUserTypes();
        UserGroup adminGroup = organizationWorkspaceProvisioner.ensureAdministratorGroup(
                organizationId, organizationClassification);
        if (adminGroup == null) {
            log.warn(
                    "Organisation {} Administrator group could not be resolved; contact user {} was not assigned",
                    organizationId,
                    user.getId());
            return;
        }
        applyGroup(user, adminGroup);
    }

    private void applyGroup(User user, UserGroup adminGroup) {
        UserGroup currentGroup = user.getUserGroup();
        if (currentGroup != null
                && currentGroup.getId() != null
                && currentGroup.getId().equals(adminGroup.getId())) {
            return;
        }
        user.setUserGroup(adminGroup);
        log.info(
                "Assigned organisation contact user {} to platform Administrator group {} (organisation {} on user row)",
                user.getId(),
                adminGroup.getId(),
                user.getOrganizationId());
    }
}
