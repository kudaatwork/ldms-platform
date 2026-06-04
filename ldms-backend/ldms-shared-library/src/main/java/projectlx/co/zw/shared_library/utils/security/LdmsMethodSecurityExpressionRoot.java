package projectlx.co.zw.shared_library.utils.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;

/**
 * {@code ROLE_ADMIN} is a platform super-role: it satisfies every {@code hasRole(...)} check on frontend APIs.
 * <p>
 * Spring Security 6+ marks {@code SecurityExpressionRoot#hasRole} as {@code final}, so this type implements
 * {@link MethodSecurityExpressionOperations} via composition instead of inheritance.
 */
public class LdmsMethodSecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private static final String ADMIN_ROLE = "ADMIN";
    /** Organisation workspace {@code Administrator} group — satisfies frontend {@code hasRole} checks (audit, users). */
    private static final String ORGANIZATION_ADMINISTRATOR_ROLE = "ORGANIZATION_ADMINISTRATOR";

    private final SecurityExpressionRoot delegate;

    private Object filterObject;
    private Object returnObject;
    private Object target;

    public LdmsMethodSecurityExpressionRoot(Authentication authentication) {
        this.delegate = new SecurityExpressionRoot(authentication) { };
    }

    public void setPermissionEvaluator(PermissionEvaluator permissionEvaluator) {
        delegate.setPermissionEvaluator(permissionEvaluator);
    }

    public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
        delegate.setTrustResolver(trustResolver);
    }

    public void setRoleHierarchy(RoleHierarchy roleHierarchy) {
        delegate.setRoleHierarchy(roleHierarchy);
    }

    public void setThis(Object target) {
        this.target = target;
    }

    @Override
    public boolean hasRole(String role) {
        return isWorkspaceSuperRole() || delegate.hasRole(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        if (isWorkspaceSuperRole()) {
            return true;
        }
        return delegate.hasAnyRole(roles);
    }

    private boolean isWorkspaceSuperRole() {
        return delegate.hasRole(ADMIN_ROLE) || delegate.hasRole(ORGANIZATION_ADMINISTRATOR_ROLE);
    }

    @Override
    public Authentication getAuthentication() {
        return delegate.getAuthentication();
    }

    @Override
    public boolean hasAuthority(String authority) {
        return delegate.hasAuthority(authority);
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        return delegate.hasAnyAuthority(authorities);
    }

    @Override
    public boolean permitAll() {
        return delegate.permitAll();
    }

    @Override
    public boolean denyAll() {
        return delegate.denyAll();
    }

    @Override
    public boolean isAnonymous() {
        return delegate.isAnonymous();
    }

    @Override
    public boolean isAuthenticated() {
        return delegate.isAuthenticated();
    }

    @Override
    public boolean isRememberMe() {
        return delegate.isRememberMe();
    }

    @Override
    public boolean isFullyAuthenticated() {
        return delegate.isFullyAuthenticated();
    }

    @Override
    public boolean hasPermission(Object target, Object permission) {
        return delegate.hasPermission(target, permission);
    }

    @Override
    public boolean hasPermission(Object targetId, String targetType, Object permission) {
        return delegate.hasPermission(targetId, targetType, permission);
    }

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }
}
