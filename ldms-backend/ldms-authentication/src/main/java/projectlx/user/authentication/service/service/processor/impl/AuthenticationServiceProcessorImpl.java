package projectlx.user.authentication.service.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.user.authentication.service.business.logic.api.AuthenticationService;
import projectlx.user.authentication.service.service.processor.api.AuthenticationServiceProcessor;
import projectlx.user.authentication.service.utils.requests.AuthRequest;
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;
import projectlx.user.authentication.service.utils.responses.AuthResponse;
import java.util.Locale;

@RequiredArgsConstructor
public class AuthenticationServiceProcessorImpl implements AuthenticationServiceProcessor {

    private final AuthenticationService authenticateService;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceProcessorImpl.class);

    @Override
    public AuthResponse authenticate(AuthRequest authRequest, Locale locale, String username) {

        logger.info("Incoming auth request : {}", authRequest);

        AuthResponse authResponse = authenticateService.authenticate(authRequest, locale, username);

        logger.info("Outgoing auth response: {}. Status Code: {}. Message: {}", authResponse,
                authResponse.getStatusCode(), authResponse.getMessage());

        return authResponse;
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest, Locale locale, String username) {

        logger.info("Incoming refresh token request : {}", refreshTokenRequest);

        AuthResponse authResponse = authenticateService.refreshToken(refreshTokenRequest, locale, username);

        logger.info("Outgoing refresh token response: {}. Status Code: {}. Message: {}", authResponse,
                authResponse.getStatusCode(), authResponse.getMessage());

        return authResponse;
    }
}
