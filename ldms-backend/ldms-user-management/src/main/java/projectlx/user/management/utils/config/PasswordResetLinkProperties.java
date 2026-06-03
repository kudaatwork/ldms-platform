package projectlx.user.management.utils.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import projectlx.co.zw.shared_library.utils.audit.AuditClientPlatformSupport;
import projectlx.user.management.model.User;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds password-reset links for admin and platform portals.
 */
@Component
public class PasswordResetLinkProperties {

    private static final String ADMIN_PORTAL = "ADMIN_PORTAL";
    private static final String PLATFORM_PORTAL = "PLATFORM_PORTAL";

    private final String adminPortalBaseUrl;
    private final String platformPortalBaseUrl;

    public PasswordResetLinkProperties(
            @Value("${ldms.user.admin-portal-base-url:http://localhost:4200}") String adminPortalBaseUrl,
            @Value("${ldms.user.organization-contact-portal-base-url:http://localhost:4201}") String platformPortalBaseUrl) {
        this.adminPortalBaseUrl = normalize(adminPortalBaseUrl, "http://localhost:4200");
        this.platformPortalBaseUrl = normalize(platformPortalBaseUrl, "http://localhost:4201");
    }

    /**
     * Picks the portal base URL from the originating client, falling back to user type when unknown.
     */
    public String resolvePortalBaseUrl(String clientPlatform, User user) {
        String normalized = AuditClientPlatformSupport.normalize(clientPlatform);
        if (ADMIN_PORTAL.equals(normalized)) {
            return adminPortalBaseUrl;
        }
        if (PLATFORM_PORTAL.equals(normalized)) {
            return platformPortalBaseUrl;
        }
        if (user != null && user.getOrganizationId() != null) {
            return platformPortalBaseUrl;
        }
        return adminPortalBaseUrl;
    }

    public String buildResetLink(String portalBaseUrl, String token, String email) {
        String base = normalize(portalBaseUrl, adminPortalBaseUrl);
        return base
                + "/auth/reset-password?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&email="
                + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    public String buildSignInUrl(String portalBaseUrl) {
        return normalize(portalBaseUrl, adminPortalBaseUrl) + "/auth/login";
    }

    private static String normalize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback.replaceAll("/+$", "");
        }
        return raw.trim().replaceAll("/+$", "");
    }
}
