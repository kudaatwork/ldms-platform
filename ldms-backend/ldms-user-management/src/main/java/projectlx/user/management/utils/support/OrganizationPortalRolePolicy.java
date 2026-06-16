package projectlx.user.management.utils.support;

import projectlx.co.zw.shared_library.utils.security.AdministratorRoleScopePolicy;

/**
 * Permission codes granted to organisation workspace administrators on the platform portal
 * (excludes LDMS platform-operator and locations-catalog roles used from the admin portal).
 */
public final class OrganizationPortalRolePolicy {

    public static final String ORGANIZATION_ADMINISTRATOR = AdministratorRoleScopePolicy.ORGANIZATION_ADMINISTRATOR;

    private OrganizationPortalRolePolicy() {
    }

    public static boolean isOrganizationPortalRole(String roleCode) {
        return AdministratorRoleScopePolicy.isOrganizationWorkspaceRole(roleCode);
    }
}
