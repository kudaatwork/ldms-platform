package projectlx.user.authentication.service.business.validation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import projectlx.user.authentication.service.business.validator.api.AuthenticationServiceValidator;
import projectlx.user.authentication.service.business.validator.impl.AuthenticationServiceValidatorImpl;
import projectlx.user.authentication.service.utils.requests.AuthRequest;
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceValidatorImplTest {

    private AuthenticationServiceValidator authenticationServiceValidator;
    private AuthRequest authRequest;
    private RefreshTokenRequest refreshTokenRequest;

    @BeforeEach
    public void setup() {
        authenticationServiceValidator = new AuthenticationServiceValidatorImpl();

        // Setup AuthRequest with valid data
        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        // Setup RefreshTokenRequest with valid data
        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setUsername("testuser");
        refreshTokenRequest.setRefreshToken("refresh-token-123");
    }

    // Tests for isAuthRequestValid method

    @Test
    public void isAuthRequestValid_shouldReturnFalseForNullRequest() {
        authRequest = null;

        boolean result = authenticationServiceValidator.isAuthRequestValid(authRequest);

        assertFalse(result, "Should return false for null request");
    }

    @Test
    public void isAuthRequestValid_shouldReturnFalseForNullUsername() {
        authRequest.setUsername(null);

        boolean result = authenticationServiceValidator.isAuthRequestValid(authRequest);

        assertFalse(result, "Should return false for null username");
    }

    @Test
    public void isAuthRequestValid_shouldReturnFalseForNullPassword() {
        authRequest.setPassword(null);

        boolean result = authenticationServiceValidator.isAuthRequestValid(authRequest);

        assertFalse(result, "Should return false for null password");
    }

    @Test
    public void isAuthRequestValid_shouldReturnFalseForEmptyUsername() {
        authRequest.setUsername("");

        boolean result = authenticationServiceValidator.isAuthRequestValid(authRequest);

        assertFalse(result, "Should return false for empty username");
    }

    @Test
    public void isAuthRequestValid_shouldReturnFalseForEmptyPassword() {
        authRequest.setPassword("");

        boolean result = authenticationServiceValidator.isAuthRequestValid(authRequest);

        assertFalse(result, "Should return false for empty password");
    }

    @Test
    public void isAuthRequestValid_shouldReturnTrueForValidRequest() {
        boolean result = authenticationServiceValidator.isAuthRequestValid(authRequest);

        assertTrue(result, "Should return true for valid request");
    }

    // Tests for isRefreshTokenRequestValid method

    @Test
    public void isRefreshTokenRequestValid_shouldReturnFalseForNullRequest() {
        refreshTokenRequest = null;

        boolean result = authenticationServiceValidator.isRefreshTokenRequestValid(refreshTokenRequest);

        assertFalse(result, "Should return false for null request");
    }

    @Test
    public void isRefreshTokenRequestValid_shouldReturnFalseForNullUsername() {
        refreshTokenRequest.setUsername(null);

        boolean result = authenticationServiceValidator.isRefreshTokenRequestValid(refreshTokenRequest);

        assertFalse(result, "Should return false for null username");
    }

    @Test
    public void isRefreshTokenRequestValid_shouldReturnFalseForNullRefreshToken() {
        refreshTokenRequest.setRefreshToken(null);

        boolean result = authenticationServiceValidator.isRefreshTokenRequestValid(refreshTokenRequest);

        assertFalse(result, "Should return false for null refresh token");
    }

    @Test
    public void isRefreshTokenRequestValid_shouldReturnFalseForEmptyUsername() {
        refreshTokenRequest.setUsername("");

        boolean result = authenticationServiceValidator.isRefreshTokenRequestValid(refreshTokenRequest);

        assertFalse(result, "Should return false for empty username");
    }

    @Test
    public void isRefreshTokenRequestValid_shouldReturnFalseForEmptyRefreshToken() {
        refreshTokenRequest.setRefreshToken("");

        boolean result = authenticationServiceValidator.isRefreshTokenRequestValid(refreshTokenRequest);

        assertFalse(result, "Should return false for empty refresh token");
    }

    @Test
    public void isRefreshTokenRequestValid_shouldReturnTrueForValidRequest() {
        boolean result = authenticationServiceValidator.isRefreshTokenRequestValid(refreshTokenRequest);

        assertTrue(result, "Should return true for valid request");
    }
}