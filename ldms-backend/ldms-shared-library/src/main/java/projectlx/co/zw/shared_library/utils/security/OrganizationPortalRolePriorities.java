package projectlx.co.zw.shared_library.utils.security;

import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * JWT {@code roles} claim compaction: organisation workspace permissions are retained first
 * when the user's group carries more roles than {@link JwtRoleClaimLimits#MAX_ROLES_IN_ACCESS_TOKEN}.
 */
public final class OrganizationPortalRolePriorities {

    public static final String ORGANIZATION_ADMINISTRATOR = "ORGANIZATION_ADMINISTRATOR";

    private static final Set<String> ORGANIZATION_MANAGEMENT = Set.of(
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

    private OrganizationPortalRolePriorities() {
    }

    public static boolean isOrganizationPortalRole(String roleCode) {
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
        return false;
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
