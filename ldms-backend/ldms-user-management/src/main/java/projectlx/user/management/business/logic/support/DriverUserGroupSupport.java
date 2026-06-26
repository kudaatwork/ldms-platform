package projectlx.user.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserGroup;
import projectlx.user.management.repository.UserGroupRepository;
import projectlx.user.management.utils.support.UserGroupNameSupport;

/**
 * Ensures that an org-scoped {@code Driver} user group exists and assigns
 * the given user to it.  Mirrors the Administrator group pattern used by
 * {@link OrganizationContactAdministratorGroupSupport}.
 */
@Component
@RequiredArgsConstructor
public class DriverUserGroupSupport {

    private static final Logger log = LoggerFactory.getLogger(DriverUserGroupSupport.class);
    public static final String DRIVER_GROUP_NAME = UserGroupNameSupport.DRIVER_GROUP_NAME;

    private final UserGroupRepository userGroupRepository;

    /**
     * Idempotently ensures a {@code Driver} user group for the given
     * organisation and returns it.
     *
     * @param organizationId the owning organisation
     * @return the existing or newly-created Driver user group, or {@code null} on error
     */
    public UserGroup ensureDriverGroup(long organizationId) {
        if (organizationId <= 0) {
            log.warn("Cannot ensure Driver group for invalid organizationId={}", organizationId);
            return null;
        }
        try {
            return userGroupRepository
                    .findByOrganizationIdAndNameIgnoreCaseAndEntityStatusNot(
                            organizationId, DRIVER_GROUP_NAME, EntityStatus.DELETED)
                    .orElseGet(() -> createDriverGroup(organizationId));
        } catch (Exception ex) {
            log.error("Failed to ensure Driver group for org {}: {}", organizationId, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Assigns the given user to the org-scoped Driver group.
     * Silently skips if the group cannot be resolved.
     *
     * @param user the user to assign
     */
    public void assignDriverGroup(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        Long organizationId = user.getOrganizationId();
        if (organizationId == null || organizationId <= 0) {
            log.warn("Driver user {} has no organisation id; skipping Driver group assignment", user.getId());
            return;
        }
        UserGroup driverGroup = ensureDriverGroup(organizationId);
        if (driverGroup == null) {
            log.warn("Driver group for org {} could not be resolved; user {} was not assigned",
                    organizationId, user.getId());
            return;
        }
        UserGroup currentGroup = user.getUserGroup();
        if (currentGroup != null && currentGroup.getId() != null
                && currentGroup.getId().equals(driverGroup.getId())) {
            return;
        }
        user.setUserGroup(driverGroup);
        log.info("Assigned driver user {} to Driver group {} (org {})",
                user.getId(), driverGroup.getId(), organizationId);
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private UserGroup createDriverGroup(long organizationId) {
        UserGroup group = new UserGroup();
        group.setName(DRIVER_GROUP_NAME);
        group.setOrganizationId(organizationId);
        group.setDescription("Fleet drivers for this organisation");
        UserGroup saved = userGroupRepository.save(group);
        log.info("Created Driver user group id={} for org {}", saved.getId(), organizationId);
        return saved;
    }
}
