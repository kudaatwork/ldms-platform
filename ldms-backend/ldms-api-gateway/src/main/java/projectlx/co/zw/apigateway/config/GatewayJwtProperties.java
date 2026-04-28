package projectlx.co.zw.apigateway.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.jwt")
public class GatewayJwtProperties {

    /**
     * HMAC secret (UTF-8); if shorter than 256 bits, it is hashed with SHA-256 for signing key material.
     */
    private String secret = "";

    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/health",
            "/actuator/info",
            "/**/swagger-ui/**",
            "/**/v3/api-docs/**",
            "/**/api/**",
            "/**/v1/system/**",
            "/**/v1/auth/**"
    ));

    private String rolesClaim = "roles";

    public String getSecret() {
        return secret;
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
