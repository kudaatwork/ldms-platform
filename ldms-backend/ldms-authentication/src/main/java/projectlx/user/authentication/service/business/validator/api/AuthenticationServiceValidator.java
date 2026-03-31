package projectlx.user.authentication.service.business.validator.api;

import projectlx.user.authentication.service.utils.requests.AuthRequest;
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;

public interface AuthenticationServiceValidator {
    boolean isAuthRequestValid(AuthRequest authRequest);
    boolean isRefreshTokenRequestValid(RefreshTokenRequest refreshTokenRequest);
}
