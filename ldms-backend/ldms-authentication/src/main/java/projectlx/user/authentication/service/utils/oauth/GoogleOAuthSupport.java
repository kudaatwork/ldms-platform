package projectlx.user.authentication.service.utils.oauth;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Validates Google OIDC ID tokens (e.g. from Google Identity Services / Sign-In on the web).
 * Requires {@code ldms.auth.google.enabled=true} and a non-empty {@code ldms.auth.google.client-id}.
 */
@Component
@ConditionalOnProperty(prefix = "ldms.auth.google", name = "enabled", havingValue = "true")
public class GoogleOAuthSupport {

	private static final String GOOGLE_ISSUER = "https://accounts.google.com";

	private final String clientId;
	private final JwtDecoder googleJwtDecoder;

	public GoogleOAuthSupport(@Value("${ldms.auth.google.client-id:}") String clientId) {
		this.clientId = clientId == null ? "" : clientId.trim();
		this.googleJwtDecoder = JwtDecoders.fromIssuerLocation(GOOGLE_ISSUER);
	}

	public boolean isConfigured() {
		return !clientId.isEmpty();
	}

	public String validateAndGetVerifiedEmail(String idToken) {
		if (!isConfigured()) {
			throw new IllegalStateException("google_client_id_missing");
		}
		Jwt jwt = googleJwtDecoder.decode(idToken);
		validateAudience(jwt, clientId);
		if (!isEmailVerified(jwt)) {
			throw new IllegalArgumentException("email_not_verified");
		}
		String email = jwt.getClaimAsString("email");
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("email_missing");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static void validateAudience(Jwt jwt, String expectedClientId) {
		List<String> audiences = jwt.getAudience();
		if (audiences == null || audiences.stream().noneMatch(expectedClientId::equals)) {
			throw new JwtException("Invalid aud claim for Google ID token");
		}
	}

	private static boolean isEmailVerified(Jwt jwt) {
		Object v = jwt.getClaim("email_verified");
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof String s) {
			return Boolean.parseBoolean(s);
		}
		return false;
	}
}
