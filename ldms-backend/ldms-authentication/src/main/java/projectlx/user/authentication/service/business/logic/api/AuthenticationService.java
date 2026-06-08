package projectlx.user.authentication.service.business.logic.api;

import projectlx.user.authentication.service.utils.requests.AuthRequest;
import projectlx.user.authentication.service.utils.requests.GoogleLoginRequest;
import projectlx.user.authentication.service.utils.requests.RefreshTokenRequest;
import projectlx.user.authentication.service.utils.requests.VerifyTwoFactorRequest;
import projectlx.user.authentication.service.utils.responses.AuthResponse;
import java.util.Locale;

public interface AuthenticationService {
    AuthResponse authenticate(AuthRequest authRequest, Locale locale, String username);
    AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest, Locale locale, String username);
    AuthResponse authenticateWithGoogle(GoogleLoginRequest googleLoginRequest, Locale locale, String actor);

    /**
     * Second step of the login 2FA challenge.
     * Validates the {@code mfaChallengeToken} (stored as a {@code MFA_CHALLENGE} token) and the
     * SMS OTP.  On success, revokes the challenge token and issues full access + refresh tokens.
     */
    AuthResponse verifyTwoFactor(VerifyTwoFactorRequest request, Locale locale);
}
