package projectlx.user.management.business.logic.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserGroup;
import projectlx.user.management.model.UserRole;
import projectlx.user.management.repository.UserGroupRepository;
import projectlx.user.management.repository.UserRoleRepository;
import projectlx.user.management.utils.support.OrganizationPortalRolePolicy;
import projectlx.user.management.utils.support.RoleClassificationPolicy;
/**
 * Editable source of truth for the organisation-classification &rarr; role mapping that powers the
 * admin-portal Group Roles Mapping drill-down.
 *
 * <p>Mutating the mapping updates {@code user_role_organization_classifications} and surgically
 * resyncs every organisation {@code Administrator} group of that classification (and therefore the
 * roles its workspace members inherit) so the whole system stays consistent.
 */
@Service
@RequiredArgsConstructor
public class ClassificationRoleService {

    private static final Logger log = LoggerFactory.getLogger(ClassificationRoleService.class);

    private final UserRoleRepository userRoleRepository;
    private final UserGroupRepository userGroupRepository;

    public record Result(boolean success, String message, int affectedRoles) {
    }

    @Transactional
    public Result assignRolesToClassification(String classification, List<Long> roleIds) {
        return mutate(classification, roleIds, true);
    }

    @Transactional
    public Result removeRolesFromClassification(String classification, List<Long> roleIds) {
        return mutate(classification, roleIds, false);
    }

    /**
     * Locks/unlocks a classification's shared default admin group. While locked, organisation admins
     * cannot assign this classification's default roles to their own org-scoped groups.
     */
    @Transactional
    public Result setLocked(String classificationRaw, boolean locked) {
        String classification = classificationRaw == null ? "" : classificationRaw.trim().toUpperCase(Locale.ROOT);
        if (!RoleClassificationPolicy.ALL_CLASSIFICATIONS.contains(classification)) {
            return new Result(false, "Unknown organisation classification: " + classificationRaw, 0);
        }
        UserGroup defaultGroup = userGroupRepository
                .findByOrganizationIdIsNullAndOrganizationClassificationIgnoreCaseAndNameIgnoreCaseAndEntityStatusNot(
                        classification, OrganizationWorkspaceProvisioner.ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED)
                .orElse(null);
        if (defaultGroup == null) {
            return new Result(false, "No default admin group exists for " + classification + ".", 0);
        }
        defaultGroup.setLocked(locked);
        userGroupRepository.save(defaultGroup);
        return new Result(true, classification + (locked ? " default admin roles locked." : " default admin roles unlocked."), 1);
    }

    private Result mutate(String classificationRaw, List<Long> roleIds, boolean assign) {
        String classification = classificationRaw == null ? "" : classificationRaw.trim().toUpperCase(Locale.ROOT);
        if (!RoleClassificationPolicy.ALL_CLASSIFICATIONS.contains(classification)) {
            return new Result(false, "Unknown organisation classification: " + classificationRaw, 0);
        }
        if (roleIds == null || roleIds.isEmpty()) {
            return new Result(false, "No roles supplied.", 0);
        }

        Set<UserRole> roles = userRoleRepository.findByIdInAndEntityStatusNot(roleIds, EntityStatus.DELETED);
        if (roles.isEmpty()) {
            return new Result(false, "No matching active roles were found.", 0);
        }

        // The single shared classification default Administrator group (organisation_id IS NULL).
        UserGroup defaultGroup = userGroupRepository
                .findByOrganizationIdIsNullAndOrganizationClassificationIgnoreCaseAndNameIgnoreCaseAndEntityStatusNot(
                        classification, OrganizationWorkspaceProvisioner.ADMINISTRATOR_GROUP_NAME, EntityStatus.DELETED)
                .orElse(null);

        int affected = 0;
        boolean defaultGroupChanged = false;
        List<String> skipped = new ArrayList<>();
        for (UserRole role : roles) {
            // Only organisation-portal roles may belong to a classification; platform-operator roles stay admin-only.
            if (!OrganizationPortalRolePolicy.isOrganizationPortalRole(role.getRole())) {
                skipped.add(role.getRole());
                continue;
            }
            boolean changed = assign
                    ? role.getOrganizationClassifications().add(classification)
                    : role.getOrganizationClassifications().remove(classification);
            if (changed) {
                userRoleRepository.save(role);
            }
            if (defaultGroup != null) {
                defaultGroupChanged |= applyRoleToDefaultGroup(defaultGroup, role, assign);
            }
            affected++;
        }
        if (defaultGroup != null && defaultGroupChanged) {
            userGroupRepository.save(defaultGroup);
        }

        if (affected == 0) {
            return new Result(false,
                    "Selected roles are platform-only and cannot be assigned to an organisation classification.", 0);
        }
        if (!skipped.isEmpty()) {
            log.info("Skipped {} platform-only role(s) for classification {}: {}",
                    skipped.size(), classification, skipped);
        }
        String verb = assign ? "assigned to" : "removed from";
        return new Result(true, affected + " role(s) " + verb + " " + classification + ".", affected);
    }

    /**
     * Adds/removes the role on the shared classification default Administrator group, keeping its
     * locked default-role set (inherited by every org admin of the classification) in sync.
     */
    private boolean applyRoleToDefaultGroup(UserGroup defaultGroup, UserRole role, boolean assign) {
        boolean changed;
        if (assign) {
            changed = defaultGroup.getUserRoles().add(role);
            if (defaultGroup.getDefaultRoleIds().add(role.getId())) {
                changed = true;
            }
        } else {
            changed = defaultGroup.getUserRoles().removeIf(r -> r.getId().equals(role.getId()));
            if (defaultGroup.getDefaultRoleIds().remove(role.getId())) {
                changed = true;
            }
        }
        return changed;
    }
}
