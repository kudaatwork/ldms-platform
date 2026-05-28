package projectlx.co.zw.apigateway.security;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.SecretKey;

/**
 * Same key derivation as {@code projectlx.co.zw.shared_library.utils.security.JwtSigningKeys}
 * (kept local so the reactive gateway does not depend on ldms-shared-library).
 */
public final class GatewayJwtSigningKeys {

    private static final int MIN_KEY_BYTES = 32;

    private GatewayJwtSigningKeys() {
    }

    public static SecretKey hmacSha256Key(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("gateway.jwt.secret must be configured");
        }
        return Keys.hmacShaKeyFor(deriveKeyBytes(configuredSecret.trim()));
    }

    private static byte[] deriveKeyBytes(String secret) {
        byte[] decoded = tryDecodeBase64(secret);
        if (decoded != null) {
            return ensureMinimumKeyLength(decoded);
        }
        return ensureMinimumKeyLength(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] tryDecodeBase64(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            try {
                return Base64.getUrlDecoder().decode(secret);
            } catch (IllegalArgumentException ignoredAgain) {
                return null;
            }
        }
    }

    private static byte[] ensureMinimumKeyLength(byte[] keyMaterial) {
        if (keyMaterial.length >= MIN_KEY_BYTES) {
            return keyMaterial;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(keyMaterial);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable for JWT key derivation.", e);
        }
    }
}
