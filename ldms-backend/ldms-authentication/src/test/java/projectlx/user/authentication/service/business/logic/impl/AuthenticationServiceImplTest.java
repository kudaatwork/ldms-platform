package projectlx.user.authentication.service.business.logic.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.authentication.service.business.auditable.api.AuthenticationServiceAuditable;
import projectlx.user.authentication.service.business.logic.api.AuthenticationService;
import projectlx.user.authentication.service.business.validator.api.AuthenticationServiceValidator;
import projectlx.user.authentication.service.clients.UserManagementServiceClient;
import projectlx.user.authentication.service.model.Token;
import projectlx.user.authentication.service.model.TokenType;
import projectlx.user.authentication.service.repository.TokenRepository;
import projectlx.user.authentication.service.utils.dtos.AuthDto;
import projectlx.user.authentication.service.utils.enums.I18Code;
import projectlx.user.authentication.service.utils.requests.AuthRequest;
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;
import projectlx.user.authentication.service.utils.responses.AuthResponse;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceImplTest {

    private AuthenticationService authenticationService;
    private AuthenticationServiceValidator authenticationServiceValidator;
    private MessageService messageService;
    private ModelMapper modelMapper;
    private TokenRepository tokenRepository;
    private AuthenticationServiceAuditable authenticationServiceAuditable;
    private UserManagementServiceClient userManagementServiceClient;
    private CustomUserDetailsServiceImpl userDetailsService;
    private JwtService jwtService;
    private AuthenticationManager authManager;

    private final Locale locale = Locale.ENGLISH;
    private final String username = "testuser";

    private AuthRequest authRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private UserDetails userDetails;
    private Token token;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        authenticationServiceValidator = mock(AuthenticationServiceValidator.class);
        messageService = mock(MessageService.class);
        modelMapper = mock(ModelMapper.class);
        tokenRepository = mock(TokenRepository.class);
        authenticationServiceAuditable = mock(AuthenticationServiceAuditable.class);
        userManagementServiceClient = mock(UserManagementServiceClient.class);
        userDetailsService = mock(CustomUserDetailsServiceImpl.class);
        jwtService = mock(JwtService.class);
        authManager = mock(AuthenticationManager.class);
        authentication = mock(Authentication.class);

        // Initialize service with mocks
        authenticationService = new AuthenticationServiceImpl(
                authenticationServiceValidator,
                messageService,
                modelMapper,
                tokenRepository,
                authenticationServiceAuditable,
                userManagementServiceClient,
                userDetailsService,
                jwtService,
                authManager
        );

        // Initialize test data
        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password");

        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setUsername("testuser");
        refreshTokenRequest.setRefreshToken("refresh-token-value");

        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");

        token = new Token();
        token.setToken("refresh-token-value");
        token.setTokenType(TokenType.BEARER);
        token.setExpired(false);
        token.setRevoked(false);
        token.setUsername("testuser");
    }

    @Test
    void authenticate_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        when(authenticationServiceValidator.isAuthRequestValid(any(AuthRequest.class))).thenReturn(false);
        when(messageService.getMessage(eq(I18Code.MESSAGE_AUTHENTICATION_INVALID_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid authentication request");

        // Act
        AuthResponse response = authenticationService.authenticate(authRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid authentication request", response.getMessage());
        assertNull(response.getAccessToken());
        assertNull(response.getRefreshToken());
    }

    @Test
    void authenticate_shouldReturnTrueAnd200ForValidRequest() {
        // Arrange
        when(authenticationServiceValidator.isAuthRequestValid(any(AuthRequest.class))).thenReturn(true);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("access-token-value");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token-value");
        when(authenticationServiceAuditable.create(any(Token.class), eq(locale), eq(username))).thenReturn(token);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_AUTHENTICATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User authenticated successfully");

        // Act
        AuthResponse response = authenticationService.authenticate(authRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User authenticated successfully", response.getMessage());
        assertEquals("access-token-value", response.getAccessToken());
        assertEquals("refresh-token-value", response.getRefreshToken());
        verify(authenticationServiceAuditable, times(1)).create(any(Token.class), eq(locale), eq(username));
    }

    @Test
    void refreshToken_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        when(authenticationServiceValidator.isRefreshTokenRequestValid(any(RefreshTokenRequest.class))).thenReturn(false);
        when(messageService.getMessage(eq(I18Code.MESSAGE_REFRESH_TOKEN_REQUEST_INVALID.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid refresh token request");

        // Act
        AuthResponse response = authenticationService.refreshToken(refreshTokenRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid refresh token request", response.getMessage());
        assertNull(response.getAccessToken());
    }

    @Test
    void refreshToken_shouldReturnFalseAnd400IfUserNotFound() {
        // Arrange
        when(authenticationServiceValidator.isRefreshTokenRequestValid(any(RefreshTokenRequest.class))).thenReturn(true);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(null);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User not found");

        // Act
        AuthResponse response = authenticationService.refreshToken(refreshTokenRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User not found", response.getMessage());
        assertNull(response.getAccessToken());
    }

    @Test
    void refreshToken_shouldReturnFalseAnd400IfTokenIsInvalid() {
        // Arrange
        when(authenticationServiceValidator.isRefreshTokenRequestValid(any(RefreshTokenRequest.class))).thenReturn(true);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(false);
        when(authenticationServiceAuditable.create(any(Token.class), eq(locale), eq(username))).thenReturn(token);
        when(messageService.getMessage(eq(I18Code.MESSAGE_REFRESH_TOKEN_REQUEST_INVALID.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid refresh token");

        // Act
        AuthResponse response = authenticationService.refreshToken(refreshTokenRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid refresh token", response.getMessage());
        assertNull(response.getAccessToken());
        verify(authenticationServiceAuditable, times(1)).create(any(Token.class), eq(locale), eq(username));
    }

    @Test
    void refreshToken_shouldReturnTrueAnd200ForValidRequest() {
        // Arrange
        when(authenticationServiceValidator.isRefreshTokenRequestValid(any(RefreshTokenRequest.class))).thenReturn(true);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.isTokenValid(anyString(), any(UserDetails.class))).thenReturn(true);
        when(jwtService.extractUsername(anyString())).thenReturn("testuser");
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("new-access-token-value");
        when(messageService.getMessage(eq(I18Code.MESSAGE_REFRESH_TOKEN_REFRESHED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Token refreshed successfully");

        // Act
        AuthResponse response = authenticationService.refreshToken(refreshTokenRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("Token refreshed successfully", response.getMessage());
        assertEquals("new-access-token-value", response.getAccessToken());
        assertNull(response.getRefreshToken());
    }
}