package projectlx.user.authentication.service.service.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;
import projectlx.user.authentication.service.utils.responses.AuthResponse;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/auth")
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
                              @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE) String username,
                              @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                      defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return authenticationServiceProcessor.authenticate(authRequest, locale, username);
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
                                     @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE) String username,
                                     @RequestHeader(value = Constants.LOCALE_LANGUAGE,
                                             defaultValue = Constants.DEFAULT_LOCALE) final Locale locale){
        return authenticationServiceProcessor.refreshToken(refreshTokenRequest, locale, username);
    }
}
