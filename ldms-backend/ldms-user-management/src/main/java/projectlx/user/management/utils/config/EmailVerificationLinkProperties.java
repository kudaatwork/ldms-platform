package projectlx.user.management.utils.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds verification links for the admin portal (browser GET, then API POST).
 */
@Component
public class EmailVerificationLinkProperties {

    private final String portalBaseUrl;

    public EmailVerificationLinkProperties(
            @Value("${ldms.user.portal-base-url:http://localhost:4200}") String portalBaseUrl) {
        this.portalBaseUrl = portalBaseUrl == null ? "http://localhost:4200" : portalBaseUrl.trim();
    }

    public String buildVerificationLink(String token, String email) {
        String base = normalizedBase();
        return base
                + "/auth/verify-email?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&email="
                + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    public String buildSignInUrl() {
        return normalizedBase() + "/auth/login";
    }

    private String normalizedBase() {
        return portalBaseUrl.replaceAll("/+$", "");
    }
}
