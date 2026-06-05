package projectlx.co.zw.organizationmanagement.utils.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.OrganizationClassification;

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

    /** Live onboarding progress page for signup applicants (public tracker). */
    public String platformOnboardingStatusUrl(Long organizationId) {
        if (organizationId == null || organizationId < 1) {
            return platformPortalBaseUrl + "/onboarding/status";
        }
        return platformPortalBaseUrl + "/onboarding/status?orgId=" + organizationId;
    }

    public String adminSignInUrl() {
        return adminPortalBaseUrl + "/auth/login";
    }

    public String buildOrganizationEmailVerificationLink(String token, String email) {
        String encodedToken = java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
        return platformPortalBaseUrl + "/auth/verify-organization-email?token=" + encodedToken + "&email=" + encodedEmail;
    }

    /** Platform portal sign-in for workspace orgs; admin portal for backoffice-only onboarding. */
    public String signInUrlFor(Organization org) {
        return usesPlatformPortal(org) ? platformSignInUrl() : adminSignInUrl();
    }

    public String nextStepsUrlFor(Organization org) {
        if (org != null && Boolean.TRUE.equals(org.getCreatedViaSignup()) && org.getId() != null && org.getId() > 0) {
            return platformOnboardingStatusUrl(org.getId());
        }
        return signInUrlFor(org);
    }

    private boolean usesPlatformPortal(Organization org) {
        if (org == null) {
            return false;
        }
        if (Boolean.TRUE.equals(org.getCreatedViaSignup())) {
            return true;
        }
        OrganizationClassification classification = org.getOrganizationClassification();
        return classification == OrganizationClassification.CUSTOMER
                || classification == OrganizationClassification.SUPPLIER
                || classification == OrganizationClassification.TRANSPORT_COMPANY;
    }

    private static String normalize(String raw, String fallback) {
        String v = raw == null || raw.isBlank() ? fallback : raw.trim();
        return v.replaceAll("/+$", "");
    }
}
