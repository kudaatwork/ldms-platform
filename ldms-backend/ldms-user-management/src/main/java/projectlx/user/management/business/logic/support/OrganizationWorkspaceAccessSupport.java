package projectlx.user.management.business.logic.support;

import java.util.Locale;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserGroup;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.utils.security.UserRoles;

/**
 * Organisation workspace access: platform operators and users with explicit LDMS roles use JWT
 * authorities; organisation members may read peers in the same {@code organization_id} when the
 * portal lists users via {@code find-by-organization-id} ({@code isAuthenticated()} only).
 */
@Component
public class OrganizationWorkspaceAccessSupport {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ADMIN = "ADMIN";
    private static final String ORGANIZATION_ADMINISTRATOR = "ORGANIZATION_ADMINISTRATOR";
    /** Admin portal backoffice surface passes this actor when no JWT principal is present. */
    private static final String BACKOFFICE_ACTOR = "BACKOFFICE";
    /** Inter-service system API calls from organisation-management and other LDMS services. */
    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final UserRepository userRepository;

    public OrganizationWorkspaceAccessSupport(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean canReadUser(String sessionUsername, Long targetUserId) {
        if (!StringUtils.hasText(sessionUsername) || targetUserId == null || targetUserId <= 0) {
            return false;
        }
        if (isTrustedServiceActorInternal(sessionUsername)
                || hasWorkspaceSuperRole()
                || hasRole(UserRoles.VIEW_USER_BY_ID.getRoleName())) {
            return true;
        }
        return sameOrganization(sessionUsername.trim(), targetUserId);
    }

    public boolean hasWorkspaceSuperRole() {
        return hasAnyRole(ADMIN, ORGANIZATION_ADMINISTRATOR);
    }

    /** Organisation id for the signed-in platform-portal workspace user, if any. */
    public Optional<Long> sessionOrganizationId(String sessionUsername) {
        if (!StringUtils.hasText(sessionUsername)) {
            return Optional.empty();
        }
        return resolveSessionUser(sessionUsername.trim()).flatMap(this::effectiveOrganizationId);
    }

    public boolean canReadOrganization(String sessionUsername, Long organizationId) {
        if (!StringUtils.hasText(sessionUsername) || organizationId == null || organizationId <= 0) {
            return false;
        }
        if (isTrustedServiceActorInternal(sessionUsername)
                || hasWorkspaceSuperRole()
                || hasRole(UserRoles.VIEW_USERS_BY_ORGANIZATION.getRoleName())) {
            return true;
        }
        return sessionOrganizationId(sessionUsername.trim())
                .map(orgId -> orgId.equals(organizationId))
                .orElse(false);
    }

    private boolean sameOrganization(String sessionPrincipal, Long targetUserId) {
        Optional<User> sessionUser = resolveSessionUser(sessionPrincipal);
        if (sessionUser.isEmpty()) {
            return false;
        }
        if (targetUserId.equals(sessionUser.get().getId())) {
            return true;
        }
        Optional<Long> sessionOrg = effectiveOrganizationId(sessionUser.get());
        if (sessionOrg.isEmpty()) {
            return false;
        }
        return userRepository
                .findByIdAndEntityStatusNot(targetUserId, EntityStatus.DELETED)
                .flatMap(this::effectiveOrganizationId)
                .map(sessionOrg.get()::equals)
                .orElse(false);
    }

    public boolean isTrustedServiceActor(String sessionUsername) {
        return isTrustedServiceActorInternal(sessionUsername);
    }

    /** Direct {@code organization_id} on the user row, else the linked workspace group's organisation. */
    private static boolean isTrustedServiceActorInternal(String sessionUsername) {
        if (!StringUtils.hasText(sessionUsername)) {
            return false;
        }
        String actor = sessionUsername.trim();
        return BACKOFFICE_ACTOR.equalsIgnoreCase(actor) || SYSTEM_ACTOR.equalsIgnoreCase(actor);
    }

    private Optional<Long> effectiveOrganizationId(User user) {
        if (user == null) {
            return Optional.empty();
        }
        Long direct = user.getOrganizationId();
        if (direct != null && direct > 0) {
            return Optional.of(direct);
        }
        UserGroup group = user.getUserGroup();
        if (group != null) {
            Long fromGroup = group.getOrganizationId();
            if (fromGroup != null && fromGroup > 0) {
                return Optional.of(fromGroup);
            }
        }
        return Optional.empty();
    }

    /**
     * Gateway or legacy clients may send numeric user id as the security principal instead of username.
     */
    private Optional<User> resolveSessionUser(String sessionPrincipal) {
        if (!StringUtils.hasText(sessionPrincipal)) {
            return Optional.empty();
        }
        String trimmed = sessionPrincipal.trim();
        Optional<User> byUsername = userRepository.findSessionProfileByUsernameIgnoreCaseAndEntityStatusNot(
                trimmed, EntityStatus.DELETED);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        try {
            long id = Long.parseLong(trimmed);
            if (id > 0) {
                return userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
            }
        } catch (NumberFormatException ignored) {
            // not a numeric principal
        }
        return Optional.empty();
    }

    private boolean hasRole(String roleCode) {
        return hasAnyRole(roleCode);
    }

    private boolean hasAnyRole(String... roleCodes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String normalized = normalizeRole(authority.getAuthority());
            for (String expected : roleCodes) {
                if (expected.equals(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String normalizeRole(String authority) {
        if (!StringUtils.hasText(authority)) {
            return "";
        }
        String role = authority.trim().toUpperCase(Locale.ROOT);
        if (role.startsWith(ROLE_PREFIX)) {
            return role.substring(ROLE_PREFIX.length());
        }
        return role;
    }
}
