package projectlx.co.zw.shared_library.utils.security;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.SecretKey;

/**
 * Derives HMAC-SHA256 signing keys from configured {@code jwt.secret} / {@code gateway.jwt.secret} values.
 * <p>
 * Supports plain-text and Base64-encoded secrets. Material shorter than 256 bits is stretched with SHA-256
 * so JJWT {@link Keys#hmacShaKeyFor(byte[])} always receives a compliant key (RFC 7518).
 */
public final class JwtSigningKeys {

    private static final int MIN_KEY_BYTES = 32;

    private JwtSigningKeys() {
    }

    public static SecretKey hmacSha256Key(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("JWT secret is blank.");
        }
        return Keys.hmacShaKeyFor(deriveKeyBytes(configuredSecret.trim()));
    }

    static byte[] deriveKeyBytes(String secret) {
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
