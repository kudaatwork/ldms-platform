package projectlx.user.management.utils.support;

import java.util.Locale;
import java.util.Set;
import projectlx.user.management.utils.security.PlatformRoles;

/**
 * Permission codes granted to organisation workspace administrators on the platform portal
 * (excludes LDMS platform-operator and locations-catalog roles used from the admin portal).
 */
public final class OrganizationPortalRolePolicy {

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
            PlatformRoles.ADMIN.getRoleName(),
            PlatformRoles.READ_ONLY.getRoleName(),
            PlatformRoles.KYC_STAGE1.getRoleName(),
            PlatformRoles.KYC_STAGE2.getRoleName(),
            PlatformRoles.KYC_STAGE3.getRoleName(),
            PlatformRoles.KYC_STAGE4.getRoleName(),
            PlatformRoles.KYC_STAGE5.getRoleName());

    private OrganizationPortalRolePolicy() {
    }

    public static boolean isOrganizationPortalRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
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
        LdmsRoleModuleResolver.RoleModule module = LdmsRoleModuleResolver.resolve(role);
        return !module.key().startsWith("locations.");
    }
}
