package projectlx.co.zw.apigateway.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "gateway.jwt")
public class GatewayJwtProperties {

    public static final String LOCAL_DEV_SECRET =
            "LDMS-local-dev-jwt-secret-at-least-thirty-two-chars-long";

    /**
     * HMAC secret (UTF-8); if shorter than 256 bits, it is hashed with SHA-256 for signing key material.
     */
    private String secret = LOCAL_DEV_SECRET;

    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/health",
            "/actuator/info",
            "/**/swagger-ui/**",
            "/**/v3/api-docs/**",
            "/**/api/**",
            "/**/v1/system/**",
            "/**/v1/auth/**",
            "/ldms-authentication/**",
            // Platform portal public signup (JSON body on frontend surface)
            "/**/v1/frontend/organization/register"
    ));

    private String rolesClaim = "roles";

    public String getSecret() {
        return StringUtils.hasText(secret) ? secret.trim() : LOCAL_DEV_SECRET;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public String getRolesClaim() {
        return rolesClaim;
    }

    public void setRolesClaim(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }
}
