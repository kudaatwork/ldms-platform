package projectlx.user.authentication.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TokenType {

    BEARER("Bearer"),

    /**
     * Short-lived challenge token issued when the user's account has 2FA enabled.
     * The client exchanges this token + the SMS OTP for full access/refresh tokens
     * via {@code POST /auth/verify-two-factor}.
     */
    MFA_CHALLENGE("MfaChallenge");

    private final String tokenType;
}
