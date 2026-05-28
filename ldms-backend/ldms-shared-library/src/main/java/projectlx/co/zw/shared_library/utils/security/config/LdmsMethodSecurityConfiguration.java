package projectlx.co.zw.shared_library.utils.security.config;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import projectlx.co.zw.shared_library.utils.security.LdmsMethodSecurityExpressionRoot;

/**
 * Supplies a custom {@link MethodSecurityExpressionHandler} so {@code @PreAuthorize("hasRole(...)")}
 * treats {@code ROLE_ADMIN} as a super-role. Requires {@code @EnableMethodSecurity} on the application.
 */
@Configuration
public class LdmsMethodSecurityConfiguration {

    @Bean
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        return new DefaultMethodSecurityExpressionHandler() {
            @Override
            protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
                    Authentication authentication, MethodInvocation invocation) {
                LdmsMethodSecurityExpressionRoot root = new LdmsMethodSecurityExpressionRoot(authentication);
                root.setThis(invocation.getThis());
                root.setPermissionEvaluator(getPermissionEvaluator());
                root.setTrustResolver(getTrustResolver());
                root.setRoleHierarchy(getRoleHierarchy());
                return root;
            }
        };
    }
}
