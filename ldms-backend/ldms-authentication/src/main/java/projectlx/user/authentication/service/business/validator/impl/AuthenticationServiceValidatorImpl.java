package projectlx.user.authentication.service.business.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.user.authentication.service.business.validator.api.AuthenticationServiceValidator;
import projectlx.user.authentication.service.utils.requests.AuthRequest;
import projectlx.user.authentication.service.utils.requests.GoogleLoginRequest;
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;

public class AuthenticationServiceValidatorImpl implements AuthenticationServiceValidator {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationServiceValidatorImpl.class);

    @Override
    public boolean isAuthRequestValid(AuthRequest authRequest) {

        if (authRequest == null) {
            logger.info("Validation failed: AuthRequest is null");
            return false;
        }

        if (authRequest.getUsername() == null || authRequest.getPassword()  == null) {
            logger.info("Validation failed: Username or password is null");
            return false;
        }

        if (authRequest.getUsername().isEmpty() || authRequest.getPassword().isEmpty()) {
            logger.info("Validation failed: Username or password is empty");
            return false;
        }

        return true;
    }

    @Override
    public boolean isRefreshTokenRequestValid(RefreshTokenRequest refreshTokenRequest) {

        if (refreshTokenRequest == null) {
            logger.info("Validation failed: RefreshTokenRequest is null");
            return false;
        }

        if (refreshTokenRequest.getUsername() == null || refreshTokenRequest.getRefreshToken()  == null) {
            logger.info("Validation failed: Username or refresh token is null");
            return false;
        }

        if (refreshTokenRequest.getUsername().isEmpty() || refreshTokenRequest.getRefreshToken().isEmpty()) {
            logger.info("Validation failed: Username or refresh token is empty");
            return false;
        }

        return true;
    }

    @Override
    public boolean isGoogleLoginRequestValid(GoogleLoginRequest googleLoginRequest) {
        if (googleLoginRequest == null) {
            logger.info("Validation failed: GoogleLoginRequest is null");
            return false;
        }
        if (googleLoginRequest.getIdToken() == null || googleLoginRequest.getIdToken().isBlank()) {
            logger.info("Validation failed: Google id token is null or blank");
            return false;
        }
        return true;
    }
}
