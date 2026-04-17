package projectlx.user.authentication.service.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.user.authentication.service.business.auditable.api.AuthenticationServiceAuditable;
import projectlx.user.authentication.service.business.logic.api.AuthenticationService;
import projectlx.user.authentication.service.business.validator.api.AuthenticationServiceValidator;
import projectlx.user.authentication.service.clients.UserManagementServiceClient;
import projectlx.user.authentication.service.model.Token;
import projectlx.user.authentication.service.model.TokenType;
import projectlx.user.authentication.service.repository.TokenRepository;
import projectlx.user.authentication.service.utils.dtos.AuthDto;
import projectlx.user.authentication.service.utils.enums.I18Code;
import projectlx.user.authentication.service.utils.oauth.GoogleOAuthSupport;
import projectlx.user.authentication.service.utils.requests.AuthRequest;
import projectlx.user.authentication.service.utils.requests.GoogleLoginRequest;
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;
import projectlx.user.authentication.service.utils.responses.AuthResponse;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationServiceValidator authenticationServiceValidator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final TokenRepository tokenRepository;
    private final AuthenticationServiceAuditable authenticationServiceAuditable;
    private final UserManagementServiceClient userManagementServiceClient;
    private final CustomUserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final Optional<GoogleOAuthSupport> googleOAuthSupport;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    @Override
    public AuthResponse authenticate(AuthRequest authRequest, Locale locale, String username) {

        String message = "";

        boolean isRequestValid = authenticationServiceValidator.isAuthRequestValid(authRequest);

        if (!isRequestValid) {

            message = messageService.getMessage(I18Code.MESSAGE_AUTHENTICATION_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildAuthResponse(400, false, message,null, null,
                    null, null, null);
        }

        try {

            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(),
                            authRequest.getPassword())
            );

        } catch (Exception ex) {
            logger.error("Error occurred while authenticating user", ex);
        }

        UserDetails user = userDetailsService.loadUserByUsername(authRequest.getUsername());

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Token refreshTokenToBeSaved = buildToken(refreshToken, authRequest.getUsername());
        Token tokenSaved = authenticationServiceAuditable.create(refreshTokenToBeSaved,
                locale, username);

        message = messageService.getMessage(I18Code.MESSAGE_USER_AUTHENTICATED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildAuthResponse(200, true, message,null, null,
                null, accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest, Locale locale, String username) {

        String message = "";

        boolean isRequestValid = authenticationServiceValidator.isRefreshTokenRequestValid(refreshTokenRequest);

        if (!isRequestValid) {

            message = messageService.getMessage(I18Code.MESSAGE_REFRESH_TOKEN_REQUEST_INVALID.getCode(),
                    new String[]{}, locale);

            return buildAuthResponse(400, false, message, null,null, null,
                    null, null);
        }

        UserDetails user = userDetailsService.loadUserByUsername(refreshTokenRequest.getUsername());

        if (user == null) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildAuthResponse(400, false, message, null,null, null,
                    null, null);
        }

        if (!jwtService.isTokenValid(refreshTokenRequest.getRefreshToken(), user)) {

            Token token = buildToken(refreshTokenRequest.getRefreshToken(), refreshTokenRequest.getUsername());
            token.setRevoked(true);
            token.setExpired(true);
            Token tokenSaved = authenticationServiceAuditable.create(token, locale, username);

            message = messageService.getMessage(I18Code.MESSAGE_REFRESH_TOKEN_REQUEST_INVALID.getCode(),
                    new String[]{}, locale);

            return buildAuthResponse(400, false, message, null,null, null,
                    null, null);
        }

        String usernameRetrieved = jwtService.extractUsername(refreshTokenRequest.getRefreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(usernameRetrieved);

        String newAccessToken = jwtService.generateToken(userDetails);

        message = messageService.getMessage(I18Code.MESSAGE_REFRESH_TOKEN_REFRESHED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildAuthResponse(200, true, message, null,null, null,
                newAccessToken, null);
    }

    @Override
    public AuthResponse authenticateWithGoogle(GoogleLoginRequest googleLoginRequest, Locale locale, String actor) {

        if (!authenticationServiceValidator.isGoogleLoginRequestValid(googleLoginRequest)) {
            String message = messageService.getMessage(I18Code.MESSAGE_AUTHENTICATION_INVALID_REQUEST.getCode(),
                    new String[] {}, locale);
            return buildAuthResponse(400, false, message, null, null, null, null, null);
        }

        if (googleOAuthSupport.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_GOOGLE_LOGIN_DISABLED.getCode(),
                    new String[] {}, locale);
            return buildAuthResponse(400, false, message, null, null, null, null, null);
        }

        GoogleOAuthSupport google = googleOAuthSupport.get();
        if (!google.isConfigured()) {
            String message = messageService.getMessage(I18Code.MESSAGE_GOOGLE_NOT_CONFIGURED.getCode(),
                    new String[] {}, locale);
            return buildAuthResponse(503, false, message, null, null, null, null, null);
        }

        final String email;
        try {
            email = google.validateAndGetVerifiedEmail(googleLoginRequest.getIdToken());
        } catch (IllegalArgumentException ex) {
            if ("email_not_verified".equals(ex.getMessage())) {
                String message = messageService.getMessage(I18Code.MESSAGE_GOOGLE_EMAIL_NOT_VERIFIED.getCode(),
                        new String[] {}, locale);
                return buildAuthResponse(400, false, message, null, null, null, null, null);
            }
            String message = messageService.getMessage(I18Code.MESSAGE_GOOGLE_INVALID_TOKEN.getCode(),
                    new String[] {}, locale);
            return buildAuthResponse(400, false, message, null, null, null, null, null);
        } catch (JwtException ex) {
            logger.warn("Google ID token rejected: {}", ex.getMessage());
            String message = messageService.getMessage(I18Code.MESSAGE_GOOGLE_INVALID_TOKEN.getCode(),
                    new String[] {}, locale);
            return buildAuthResponse(401, false, message, null, null, null, null, null);
        } catch (IllegalStateException ex) {
            String message = messageService.getMessage(I18Code.MESSAGE_GOOGLE_NOT_CONFIGURED.getCode(),
                    new String[] {}, locale);
            return buildAuthResponse(503, false, message, null, null, null, null, null);
        } catch (RuntimeException ex) {
            logger.error("Unexpected error validating Google token", ex);
            String message = messageService.getMessage(I18Code.MESSAGE_GOOGLE_INVALID_TOKEN.getCode(),
                    new String[] {}, locale);
            return buildAuthResponse(401, false, message, null, null, null, null, null);
        }

        UserResponse userResponse = resolveLdmsUserByGoogleEmail(email);
        if (userResponse == null || !userResponse.isSuccess() || userResponse.getUserDto() == null
                || userResponse.getUserDto().getUsername() == null
                || userResponse.getUserDto().getUsername().isBlank()) {
            String message = messageService.getMessage(I18Code.MESSAGE_GOOGLE_USER_NOT_FOUND.getCode(),
                    new String[] {}, locale);
            return buildAuthResponse(404, false, message, null, null, null, null, null);
        }

        String ldmsUsername = userResponse.getUserDto().getUsername();
        final UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(ldmsUsername);
        } catch (UsernameNotFoundException ex) {
            String message = messageService.getMessage(I18Code.MESSAGE_GOOGLE_USER_NOT_FOUND.getCode(),
                    new String[] {}, locale);
            return buildAuthResponse(404, false, message, null, null, null, null, null);
        }
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Token refreshTokenToBeSaved = buildToken(refreshToken, ldmsUsername);
        authenticationServiceAuditable.create(refreshTokenToBeSaved, locale, actor != null ? actor : ldmsUsername);

        String message = messageService.getMessage(I18Code.MESSAGE_USER_AUTHENTICATED_SUCCESSFULLY.getCode(),
                new String[] {}, locale);

        return buildAuthResponse(200, true, message, null, null, null, accessToken, refreshToken);
    }

    private UserResponse resolveLdmsUserByGoogleEmail(String email) {
        UserResponse byEmail = userManagementServiceClient.findByPhoneNumberOrEmail(email);
        if (byEmail != null && byEmail.isSuccess() && byEmail.getUserDto() != null) {
            return byEmail;
        }
        return userManagementServiceClient.findByUsername(email);
    }

    private Token buildToken(String refreshToken, String username){

        Token token = new Token();
        token.setToken(refreshToken);
        token.setTokenType(TokenType.BEARER);
        token.setExpired(false);
        token.setRevoked(false);
        token.setUsername(username);

        return token;
    }

    private AuthResponse buildAuthResponse(int statusCode, boolean isSuccess, String message,
                                           AuthDto authDto, List<AuthDto> authDtoList,
                                           Page<AuthDto> authDtoPage, String accessToken,
                                           String refreshToken){

        AuthResponse authResponse = new AuthResponse();
        authResponse.setStatusCode(statusCode);
        authResponse.setSuccess(isSuccess);
        authResponse.setMessage(message);
        authResponse.setAuthDto(authDto);
        authResponse.setAuthDtoList(authDtoList);
        authResponse.setAuthDtoPage(authDtoPage);
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(refreshToken);

        return authResponse;
    }
}
