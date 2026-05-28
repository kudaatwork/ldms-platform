package projectlx.co.zw.shared_library.utils.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Shared JWT settings — must match {@code gateway.jwt.secret} and ldms-authentication {@code jwt.secret}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    public static final String LOCAL_DEV_SECRET =
            "LDMS-local-dev-jwt-secret-at-least-thirty-two-chars-long";

    private String secret = LOCAL_DEV_SECRET;

    private long accessTokenExpirationMs = 900_000L;

    private long refreshTokenExpirationMs = 604_800_000L;

    /** Never return blank (empty env {@code JWT_SECRET} breaks HS256 validation). */
    public String getSecret() {
        return StringUtils.hasText(secret) ? secret.trim() : LOCAL_DEV_SECRET;
    }
}
