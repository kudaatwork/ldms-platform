package projectlx.user.authentication.service.service.processor.api;

import projectlx.user.authentication.service.utils.requests.AuthRequest;
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;
import projectlx.user.authentication.service.utils.responses.AuthResponse;
import java.util.Locale;

public interface AuthenticationServiceProcessor {
    AuthResponse authenticate(AuthRequest authRequest, Locale locale, String username);
    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest, Locale locale, String username);
}
