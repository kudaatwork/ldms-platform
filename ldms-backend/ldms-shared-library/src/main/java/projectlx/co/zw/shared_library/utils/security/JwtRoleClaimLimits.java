package projectlx.co.zw.shared_library.utils.security;

/** Limits for embedding role codes in access-token JWT claims. */
public final class JwtRoleClaimLimits {

    /** Organisation administrators can hold ~70 workspace roles; stay below typical proxy header limits. */
    public static final int MAX_ROLES_IN_ACCESS_TOKEN = 96;

    private JwtRoleClaimLimits() {
    }
}
