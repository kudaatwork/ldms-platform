package projectlx.co.zw.shared_library.utils.security;

/**
 * JWT {@code roles} claim compaction: organisation workspace permissions are retained first
 * when the user's group carries more roles than {@link JwtRoleClaimLimits#MAX_ROLES_IN_ACCESS_TOKEN}.
 */
public final class OrganizationPortalRolePriorities {

    public static final String ORGANIZATION_ADMINISTRATOR = AdministratorRoleScopePolicy.ORGANIZATION_ADMINISTRATOR;

    private OrganizationPortalRolePriorities() {
    }

    public static boolean isOrganizationPortalRole(String roleCode) {
        return AdministratorRoleScopePolicy.isOrganizationPortalRole(roleCode);
    }
}
