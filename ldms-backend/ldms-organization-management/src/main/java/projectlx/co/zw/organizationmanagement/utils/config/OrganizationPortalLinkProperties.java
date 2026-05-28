package projectlx.co.zw.organizationmanagement.utils.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Public URLs included in organisation registration emails.
 */
@Component
public class OrganizationPortalLinkProperties {

    private final String platformPortalBaseUrl;
    private final String adminPortalBaseUrl;

    public OrganizationPortalLinkProperties(
            @Value("${ldms.organization.platform-portal-base-url:http://localhost:4201}") String platformPortalBaseUrl,
            @Value("${ldms.organization.admin-portal-base-url:http://localhost:4200}") String adminPortalBaseUrl) {
        this.platformPortalBaseUrl = normalize(platformPortalBaseUrl, "http://localhost:4201");
        this.adminPortalBaseUrl = normalize(adminPortalBaseUrl, "http://localhost:4200");
    }

    public String platformSignInUrl() {
        return platformPortalBaseUrl + "/auth/login";
    }

    public String platformSignupUrl() {
        return platformPortalBaseUrl + "/signup";
    }

    public String adminSignInUrl() {
        return adminPortalBaseUrl + "/auth/login";
    }

    private static String normalize(String raw, String fallback) {
        String v = raw == null || raw.isBlank() ? fallback : raw.trim();
        return v.replaceAll("/+$", "");
    }
}
