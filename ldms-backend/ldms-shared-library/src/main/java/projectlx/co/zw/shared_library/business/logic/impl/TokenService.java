package projectlx.co.zw.shared_library.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import projectlx.co.zw.shared_library.utils.generators.SecureTokenGenerator;

/**
 * Service layer for token operations.
 * 
 * This service provides a clean interface for token generation and validation
 * while encapsulating the underlying token generator implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final SecureTokenGenerator tokenGenerator;

    /**
     * Generates a new verification token for user email verification.
     *
     * @return A secure verification token
     */
    public String generateEmailVerificationToken() {
        log.info("Generating email verification token");
        return tokenGenerator.generateVerificationToken();
    }

    /**
     * Generates a new password reset token.
     *
     * @return A secure password reset token
     */
    public String generatePasswordResetToken() {
        log.info("Generating password reset token");
        return tokenGenerator.generateVerificationToken();
    }

    /**
     * Validates a verification token.
     *
     * @param token The token to validate
     * @return TokenValidationResult with validation details
     */
    public SecureTokenGenerator.TokenValidationResult validateVerificationToken(String token) {
        log.info("Validating verification token");
        return tokenGenerator.validateToken(token);
    }

    /**
     * Checks if a token is valid and not expired.
     *
     * @param token The token to check
     * @return true if token is valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        SecureTokenGenerator.TokenValidationResult result = validateVerificationToken(token);
        return result.isValid();
    }

    /**
     * Checks if a token is expired.
     *
     * @param token The token to check
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        SecureTokenGenerator.TokenValidationResult result = validateVerificationToken(token);
        return result.isExpired();
    }
}