package projectlx.co.zw.shared_library.utils.generators;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Enterprise-grade token generator for creating secure verification tokens.
 * 
 * This utility class provides cryptographically secure token generation
 * with configurable algorithms, expiration times, and validation capabilities.
 * 
 * Features:
 * - Cryptographically secure random generation
 * - HMAC-based token integrity
 * - Configurable token length and expiration
 * - Thread-safe implementation
 * - Comprehensive logging and error handling
 * 
 * @author Project LX Team
 * @version 1.0
 * @since 2025-07-22
 */
@Slf4j
@Component
public class SecureTokenGenerator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_DELIMITER = ".";
    private static final int DEFAULT_TOKEN_LENGTH = 32;
    private static final long DEFAULT_EXPIRATION_HOURS = 24;

    private final SecureRandom secureRandom;
    private final String secretKey;
    private final int tokenLength;
    private final long expirationHours;

    /**
     * Constructor with configurable parameters from application properties.
     *
     * @param secretKey       Secret key for HMAC signing (from application.yml)
     * @param tokenLength     Length of the random token part (default: 32)
     * @param expirationHours Token expiration time in hours (default: 24)
     */
    public SecureTokenGenerator(
            @Value("${app.security.token.secret-key:defaultSecretKeyChangeInProduction}") String secretKey,
            @Value("${app.security.token.length:32}") int tokenLength,
            @Value("${app.security.token.expiration-hours:24}") long expirationHours) {
        
        this.secretKey = Objects.requireNonNull(secretKey, "Secret key cannot be null");
        this.tokenLength = Math.max(tokenLength, 16); // Minimum 16 characters
        this.expirationHours = Math.max(expirationHours, 1); // Minimum 1 hour
        this.secureRandom = new SecureRandom();
        
        log.info("SecureTokenGenerator initialized with token length: {} and expiration: {} hours", 
                this.tokenLength, this.expirationHours);
    }

    /**
     * Generates a secure verification token with the following structure:
     * {randomToken}.{timestamp}.{hmacSignature}
     *
     * @return A cryptographically secure verification token
     * @throws TokenGenerationException if token generation fails
     */
    public String generateVerificationToken() {
        try {
            log.debug("Generating new verification token");
            
            // Generate cryptographically secure random token
            String randomToken = generateSecureRandomToken();
            
            // Add timestamp for expiration tracking
            long timestamp = Instant.now().toEpochMilli();
            
            // Create payload: randomToken.timestamp
            String payload = randomToken + TOKEN_DELIMITER + timestamp;
            
            // Generate HMAC signature for integrity
            String signature = generateHmacSignature(payload);
            
            // Final token: randomToken.timestamp.signature
            String finalToken = payload + TOKEN_DELIMITER + signature;
            
            log.debug("Verification token generated successfully with length: {}", finalToken.length());
            return finalToken;
            
        } catch (Exception e) {
            log.error("Failed to generate verification token", e);
            throw new TokenGenerationException("Token generation failed", e);
        }
    }

    /**
     * Generates a simple secure random token (backward compatibility).
     *
     * @return A secure random token string
     */
    public String generateSimpleToken() {
        log.debug("Generating simple secure token");
        return generateSecureRandomToken();
    }

    /**
     * Validates a verification token by checking its integrity and expiration.
     *
     * @param token The token to validate
     * @return TokenValidationResult containing validation status and details
     */
    public TokenValidationResult validateToken(String token) {
        try {
            log.debug("Validating token");
            
            if (token == null || token.trim().isEmpty()) {
                return TokenValidationResult.invalid("Token is null or empty");
            }

            String[] parts = token.split("\\" + TOKEN_DELIMITER);
            if (parts.length != 3) {
                return TokenValidationResult.invalid("Invalid token format");
            }

            String randomToken = parts[0];
            String timestampStr = parts[1];
            String providedSignature = parts[2];

            // Validate timestamp
            long timestamp;
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                return TokenValidationResult.invalid("Invalid timestamp format");
            }

            // Check expiration
            long currentTime = Instant.now().toEpochMilli();
            long expirationTime = timestamp + (expirationHours * 60 * 60 * 1000);
            
            if (currentTime > expirationTime) {
                return TokenValidationResult.expired("Token has expired");
            }

            // Validate HMAC signature
            String payload = randomToken + TOKEN_DELIMITER + timestamp;
            String expectedSignature = generateHmacSignature(payload);
            
            if (!expectedSignature.equals(providedSignature)) {
                return TokenValidationResult.invalid("Invalid token signature");
            }

            log.debug("Token validation successful");
            return TokenValidationResult.valid();

        } catch (Exception e) {
            log.error("Error during token validation", e);
            return TokenValidationResult.invalid("Token validation error: " + e.getMessage());
        }
    }

    /**
     * Generates a cryptographically secure random token.
     *
     * @return Base64-encoded secure random token
     */
    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[tokenLength];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Generates HMAC signature for the given payload.
     *
     * @param payload The payload to sign
     * @return Base64-encoded HMAC signature
     * @throws TokenGenerationException if HMAC generation fails
     */
    private String generateHmacSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new TokenGenerationException("HMAC signature generation failed", e);
        }
    }

    /**
     * Result object for token validation operations.
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final boolean expired;
        private final String message;

        private TokenValidationResult(boolean valid, boolean expired, String message) {
            this.valid = valid;
            this.expired = expired;
            this.message = message;
        }

        public static TokenValidationResult valid() {
            return new TokenValidationResult(true, false, "Token is valid");
        }

        public static TokenValidationResult invalid(String message) {
            return new TokenValidationResult(false, false, message);
        }

        public static TokenValidationResult expired(String message) {
            return new TokenValidationResult(false, true, message);
        }

        public boolean isValid() { return valid; }
        public boolean isExpired() { return expired; }
        public String getMessage() { return message; }
    }

    /**
     * Custom exception for token generation failures.
     */
    public static class TokenGenerationException extends RuntimeException {
        public TokenGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}