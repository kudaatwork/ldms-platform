package projectlx.user.authentication.service.service.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.authentication.service.service.processor.api.AuthenticationServiceProcessor;
import projectlx.user.authentication.service.utils.requests.AuthRequest;
import projectlx.user.authentication.service.utils.requests.GoogleLoginRequest;
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;
import projectlx.user.authentication.service.utils.responses.AuthResponse;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-authentication/v1/auth")
@Tag(name = "Auth Resource", description = "Operations related to managing authentication")
@RequiredArgsConstructor
public class AuthenticationResource {

    private final AuthenticationServiceProcessor authenticationServiceProcessor;

    @Auditable(action = "USER_AUTHENTICATION")
    @PostMapping("/request-access-token")
    @Operation(summary = "User Authentication", description = "Authenticates user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User authenticated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public AuthResponse login(@Valid @RequestBody final AuthRequest authRequest,
                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return authenticationServiceProcessor.authenticate(authRequest, locale, authRequest.getUsername());
    }

    @Auditable(action = "REFRESH_TOKEN")
    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh token", description = "Refreshes access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public AuthResponse refreshToken(@Valid @RequestBody final RefreshTokenRequest refreshTokenRequest,
                                     @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                             defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return authenticationServiceProcessor.refreshToken(
                refreshTokenRequest,
                locale,
                refreshTokenRequest.getUsername()
        );
    }

    @Auditable(action = "USER_AUTHENTICATION_GOOGLE")
    @PostMapping("/google-id-token")
    @Operation(summary = "Google sign-in", description = "Exchanges a Google OIDC ID token for LDMS access and refresh tokens. "
            + "The Google account email must match an existing user (by email or username). Requires ldms.auth.google.enabled=true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User authenticated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request, Google login disabled, or email not verified"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired Google ID token"),
            @ApiResponse(responseCode = "404", description = "No LDMS user linked to this Google account"),
            @ApiResponse(responseCode = "503", description = "Google OAuth not fully configured"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public AuthResponse loginWithGoogle(@Valid @RequestBody final GoogleLoginRequest googleLoginRequest,
                                        @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                                defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return authenticationServiceProcessor.authenticateWithGoogle(googleLoginRequest, locale, "GOOGLE_OIDC");
    }
}
