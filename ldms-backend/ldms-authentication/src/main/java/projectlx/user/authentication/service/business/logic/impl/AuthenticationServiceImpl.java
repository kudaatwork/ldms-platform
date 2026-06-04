package projectlx.user.authentication.service.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.user.authentication.service.business.auditable.api.AuthenticationServiceAuditable;
import projectlx.user.authentication.service.business.logic.api.AuthenticationService;
import projectlx.user.authentication.service.business.logic.support.LoginIdentifierResolver;
import projectlx.user.authentication.service.business.logic.support.ResolvedLogin;
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
import feign.FeignException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.StringUtils;

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

        final String loginId = authRequest.getUsername().trim();
        final ResolvedLogin resolvedLogin;
        try {
            resolvedLogin = LoginIdentifierResolver.resolve(userManagementServiceClient, loginId);
        } catch (FeignException ex) {
            logger.error("User management unavailable while resolving login id", ex);
            message = "User management service is unavailable. Start ldms-user-management (port 8086) and retry.";
            return buildAuthResponse(503, false, message, null, null, null, null, null);
        }
        if (resolvedLogin == null || !StringUtils.hasText(resolvedLogin.username())) {
            logger.warn("Login identifier could not be resolved: {}", loginId);
            message = messageService.getMessage(I18Code.MESSAGE_AUTHENTICATION_USER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildAuthResponse(401, false, message, null, null, null, null, null);
        }

        final String resolvedUsername = resolvedLogin.username();
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(resolvedUsername, authRequest.getPassword()));
            UserDetails user = (UserDetails) authentication.getPrincipal();
            Map<String, Object> claims = buildTokenClaims(resolvedLogin.userDto(), resolvedUsername);

            String accessToken = jwtService.generateToken(user, claims);
            String refreshToken = jwtService.generateRefreshToken(user);

            Token refreshTokenToBeSaved = buildToken(refreshToken, resolvedUsername);
            authenticationServiceAuditable.create(refreshTokenToBeSaved, locale, username);

            message = messageService.getMessage(I18Code.MESSAGE_USER_AUTHENTICATED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);

            boolean mustChangeCredentials = resolvedLogin.userDto() != null
                    && Boolean.TRUE.equals(resolvedLogin.userDto().getMustChangeCredentials());

            return buildAuthResponse(200, true, message, null, null,
                    null, accessToken, refreshToken, mustChangeCredentials);
        } catch (FeignException ex) {
            logger.error("User management unavailable during login for {}", resolvedUsername, ex);
            message = "User management service is unavailable. Start ldms-user-management (port 8086) and retry.";
            return buildAuthResponse(503, false, message, null, null, null, null, null);
        } catch (BadCredentialsException ex) {
            logger.warn("Bad credentials for {}", resolvedUsername);
            message = messageService.getMessage(I18Code.MESSAGE_AUTHENTICATION_BAD_CREDENTIALS.getCode(),
                    new String[]{}, locale);
            return buildAuthResponse(401, false, message, null, null, null, null, null);
        } catch (UsernameNotFoundException ex) {
            logger.warn("User profile unavailable for {}: {}", resolvedUsername, ex.getMessage());
            message = messageService.getMessage(I18Code.MESSAGE_AUTHENTICATION_USER_NOT_FOUND.getCode(),
                    new String[]{}, locale);
            return buildAuthResponse(401, false, message, null, null, null, null, null);
        } catch (AuthenticationException ex) {
            logger.warn("Authentication failed for {}: {}", resolvedUsername, ex.getMessage());
            message = messageService.getMessage(I18Code.MESSAGE_AUTHENTICATION_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildAuthResponse(401, false, message, null, null, null, null, null);
        } catch (Exception ex) {
            logger.warn("Unexpected error during login for {}", resolvedUsername, ex);
            message = messageService.getMessage(I18Code.MESSAGE_AUTHENTICATION_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildAuthResponse(401, false, message, null, null, null, null, null);
        }
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
        Map<String, Object> claims = buildTokenClaims(null, usernameRetrieved);

        String newAccessToken = jwtService.generateToken(userDetails, claims);

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
        Map<String, Object> claims = buildTokenClaims(userResponse.getUserDto(), ldmsUsername);
        String accessToken = jwtService.generateToken(user, claims);
        String refreshToken = jwtService.generateRefreshToken(user);

        Token refreshTokenToBeSaved = buildToken(refreshToken, ldmsUsername);
        authenticationServiceAuditable.create(refreshTokenToBeSaved, locale, actor != null ? actor : ldmsUsername);

        String message = messageService.getMessage(I18Code.MESSAGE_USER_AUTHENTICATED_SUCCESSFULLY.getCode(),
                new String[] {}, locale);

        return buildAuthResponse(200, true, message, null, null, null, accessToken, refreshToken);
    }

    private Map<String, Object> buildTokenClaims(UserDto cachedUser, String username) {
        Map<String, Object> claims = new HashMap<>();
        UserDto dto = cachedUser;
        if (dto == null) {
            UserResponse userResponse = userManagementServiceClient.findByUsername(username);
            if (userResponse == null || !userResponse.isSuccess() || userResponse.getUserDto() == null) {
                return claims;
            }
            dto = userResponse.getUserDto();
        }
        if (dto.getId() != null) {
            claims.put("userId", dto.getId());
        }
        if (StringUtils.hasText(dto.getEmail())) {
            claims.put("email", dto.getEmail().trim());
        }
        if (StringUtils.hasText(dto.getFirstName())) {
            claims.put("firstName", dto.getFirstName().trim());
        }
        if (StringUtils.hasText(dto.getLastName())) {
            claims.put("lastName", dto.getLastName().trim());
        }
        if (dto.getOrganizationId() != null) {
            claims.put("organizationId", dto.getOrganizationId());
        }
        if (Boolean.TRUE.equals(dto.getOrganizationKycApprover())) {
            claims.put("organizationKycApprover", true);
        }
        if (Boolean.TRUE.equals(dto.getOperationalIssueHandler())) {
            claims.put("operationalIssueHandler", true);
        }
        if (Boolean.TRUE.equals(dto.getMustChangeCredentials())) {
            claims.put("mustChangeCredentials", true);
        }
        return claims;
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
        return buildAuthResponse(statusCode, isSuccess, message, authDto, authDtoList, authDtoPage, accessToken,
                refreshToken, null);
    }

    private AuthResponse buildAuthResponse(int statusCode, boolean isSuccess, String message,
                                           AuthDto authDto, List<AuthDto> authDtoList,
                                           Page<AuthDto> authDtoPage, String accessToken,
                                           String refreshToken, Boolean mustChangeCredentials){

        AuthResponse authResponse = new AuthResponse();
        authResponse.setStatusCode(statusCode);
        authResponse.setSuccess(isSuccess);
        authResponse.setMessage(message);
        authResponse.setAuthDto(authDto);
        authResponse.setAuthDtoList(authDtoList);
        authResponse.setAuthDtoPage(authDtoPage);
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(refreshToken);
        authResponse.setMustChangeCredentials(mustChangeCredentials);

        return authResponse;
    }
}
