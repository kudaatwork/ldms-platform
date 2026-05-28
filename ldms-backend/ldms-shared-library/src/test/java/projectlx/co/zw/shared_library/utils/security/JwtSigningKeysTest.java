package projectlx.co.zw.shared_library.utils.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtSigningKeysTest {

    @Test
    void shortBase64DecodedSecretIsStretchedToThirtyTwoBytes() {
        byte[] shortKey = new byte[30];
        String base64Secret = Base64.getEncoder().encodeToString(shortKey);

        byte[] derived = JwtSigningKeys.deriveKeyBytes(base64Secret);

        assertEquals(32, derived.length);
        assertDoesNotThrow(() -> JwtSigningKeys.hmacSha256Key(base64Secret));
    }

    @Test
    void shortPlainTextSecretIsStretchedToThirtyTwoBytes() {
        String plainSecret = "too-short-plain-secret";

        byte[] derived = JwtSigningKeys.deriveKeyBytes(plainSecret);

        assertEquals(32, derived.length);
        assertDoesNotThrow(() -> JwtSigningKeys.hmacSha256Key(plainSecret));
    }
}
