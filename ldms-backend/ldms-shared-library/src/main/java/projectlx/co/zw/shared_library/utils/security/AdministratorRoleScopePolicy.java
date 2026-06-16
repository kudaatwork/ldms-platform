package projectlx.co.zw.shared_library.utils.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * One {@code Administrator} user group holds the full LDMS role catalog; effective JWT / session roles
 * depend on whether the user is a platform operator ({@code organizationId} null) or an organisation
 * workspace member ({@code organizationId} set).
 */
public final class AdministratorRoleScopePolicy {

    public static final String ORGANIZATION_ADMINISTRATOR = "ORGANIZATION_ADMINISTRATOR";
    public static final String ADMINISTRATOR_GROUP_NAME = "Administrator";

    private static final Set<String> ORGANIZATION_EXCLUSIVE = Set.of(
            ORGANIZATION_ADMINISTRATOR,
            "SUBMIT_KYC",
            "VIEW_MY_ORGAN",
            "UPDATE_MY_ORGAN",
            "MANAGE_BRANCHES",
            "LIST_CUSTOMERS",
            "REGISTER_CUSTOMER",
            "LINK_TRANSPORTER");

    private static final Set<String> PLATFORM_OPERATOR = Set.of(
            "ADMIN",
            "READ_ONLY",
            "KYC_STAGE1",
            "KYC_STAGE2",
            "KYC_STAGE3",
            "KYC_STAGE4",
            "KYC_STAGE5");

    private static final Set<String> ORGANIZATION_MANAGEMENT = Set.of(
            "SUBMIT_KYC",
            "VIEW_MY_ORGAN",
            "UPDATE_MY_ORGAN",
            "MANAGE_BRANCHES",
            "LIST_CUSTOMERS",
            "REGISTER_CUSTOMER",
            "LINK_TRANSPORTER");

    public enum PortalScope {
        PLATFORM_OPERATOR,
        ORGANIZATION_WORKSPACE
    }

    private AdministratorRoleScopePolicy() {
    }

    public static PortalScope portalScopeFor(Long organizationId) {
        if (organizationId != null && organizationId > 0) {
            return PortalScope.ORGANIZATION_WORKSPACE;
        }
        return PortalScope.PLATFORM_OPERATOR;
    }

    public static List<String> filterRoleCodesForUser(Collection<String> roleCodes, Long organizationId) {
        return filterRoleCodes(roleCodes, portalScopeFor(organizationId));
    }

    public static List<String> filterRoleCodes(Collection<String> roleCodes, PortalScope scope) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return scope == PortalScope.ORGANIZATION_WORKSPACE
                    ? List.of(ORGANIZATION_ADMINISTRATOR)
                    : List.of();
        }
        LinkedHashSet<String> filtered = new LinkedHashSet<>();
        for (String raw : roleCodes) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String role = raw.trim().toUpperCase(Locale.ROOT);
            if (isEffectiveForScope(role, scope)) {
                filtered.add(role);
            }
        }
        if (scope == PortalScope.ORGANIZATION_WORKSPACE) {
            filtered.add(ORGANIZATION_ADMINISTRATOR);
        }
        return List.copyOf(filtered);
    }

    public static boolean isEffectiveForScope(String roleCode, PortalScope scope) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        return scope == PortalScope.ORGANIZATION_WORKSPACE
                ? isOrganizationWorkspaceRole(roleCode)
                : isPlatformOperatorRole(roleCode);
    }

    /** Roles for LDMS admin portal operators (no organisation workspace on the user row). */
    public static boolean isPlatformOperatorRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        String role = roleCode.trim().toUpperCase(Locale.ROOT);
        return !ORGANIZATION_EXCLUSIVE.contains(role);
    }

    /** Roles for platform portal organisation workspace administrators and members. */
    public static boolean isOrganizationWorkspaceRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        String role = roleCode.trim().toUpperCase(Locale.ROOT);
        if (ORGANIZATION_ADMINISTRATOR.equals(role)) {
            return true;
        }
        if (PLATFORM_OPERATOR.contains(role)) {
            return false;
        }
        if ("CHURN_OUT_AUDIT_LOGS".equals(role)) {
            return false;
        }
        if (ORGANIZATION_MANAGEMENT.contains(role)) {
            return true;
        }
        if (role.contains("AUDIT_LOG") || role.startsWith("SEARCH_AUDIT")) {
            return true;
        }
        if ("ASSIGN_USER_ROLES_TO_USER_GROUP".equals(role) || "REMOVE_USER_ROLES_FROM_USER_GROUP".equals(role)) {
            return true;
        }
        if (role.contains("USER_")) {
            return true;
        }
        if (role.contains("TEMPLATE") || role.contains("NOTIFICATION_LOG")) {
            return false;
        }
        if (isLocationCatalogRole(role)) {
            return false;
        }
        return true;
    }

    /** Used by JWT compaction — organisation workspace permissions first. */
    public static boolean isOrganizationPortalRole(String roleCode) {
        return isOrganizationWorkspaceRole(roleCode);
    }

    private static boolean isLocationCatalogRole(String role) {
        if (role.contains("USER_ADDRESS")) {
            return false;
        }
        return role.contains("_ADDRESS")
                || role.contains("ADMINISTRATIVE_LEVEL")
                || role.contains("_CITY")
                || role.contains("_CITIES")
                || role.contains("_COUNTRY")
                || role.contains("_COUNTRIES")
                || role.contains("_DISTRICT")
                || role.contains("_DISTRICTS")
                || role.contains("GEO_COORDINATES")
                || role.contains("_LANGUAGE")
                || role.contains("_LANGUAGES")
                || role.contains("LOCALIZED_NAME")
                || role.contains("LOCATION_NODE")
                || role.contains("_PROVINCE")
                || role.contains("_PROVINCES")
                || role.contains("_SUBURB")
                || role.contains("_SUBURBS")
                || role.contains("_VILLAGE")
                || role.contains("_VILLAGES");
    }
}
