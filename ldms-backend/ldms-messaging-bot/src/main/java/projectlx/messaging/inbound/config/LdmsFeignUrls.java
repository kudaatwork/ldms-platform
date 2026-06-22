package projectlx.messaging.inbound.config;

import org.springframework.core.env.Environment;

import java.net.URI;

public final class LdmsFeignUrls {

    private LdmsFeignUrls() {}

    public static String resolveUserManagementServiceBaseUrl(Environment env) {
        return resolveServiceUrl(env, "CLIENTS_USER_MANAGEMENT_SERVICE_URL", "clients.base-url.userManagementService");
    }

    public static String resolveOrganizationManagementServiceBaseUrl(Environment env) {
        return resolveServiceUrl(env, "CLIENTS_ORGANIZATION_MANAGEMENT_SERVICE_URL",
                "clients.base-url.organizationManagementService");
    }

    public static String resolveBillingPaymentsServiceBaseUrl(Environment env) {
        return resolveServiceUrl(env, "CLIENTS_BILLING_PAYMENTS_SERVICE_URL", "clients.base-url.billingPaymentsService");
    }

    public static String resolveApiGatewayBaseUrl(Environment env) {
        String explicit = env.getProperty("CLIENTS_API_GATEWAY_URL");
        if (explicit != null && !explicit.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(explicit.trim()));
        }
        String configured = env.getProperty("clients.base-url.apiGateway");
        if (configured != null && !configured.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(configured));
        }
        String ldmsGateway = env.getProperty("ldms.api-gateway.base-url");
        if (ldmsGateway != null && !ldmsGateway.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(ldmsGateway));
        }
        String host = env.getProperty("API_GATEWAY_HOST", "127.0.0.1");
        String port = env.getProperty("GATEWAY_SERVER_PORT", "8091");
        return "http://" + host + ":" + port;
    }

    private static String resolveServiceUrl(Environment env, String explicitKey, String configuredKey) {
        String explicit = env.getProperty(explicitKey);
        if (explicit != null && !explicit.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(explicit.trim()));
        }
        if (Boolean.parseBoolean(env.getProperty("ldms.dev.force-local-feign-clients", "false"))) {
            return resolveApiGatewayBaseUrl(env);
        }
        String configured = env.getProperty(configuredKey);
        if (configured != null && !configured.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(configured));
        }
        return resolveApiGatewayBaseUrl(env);
    }

    private static String extractHttpOrigin(String url) {
        try {
            URI u = URI.create(url);
            String scheme = u.getScheme();
            String host = u.getHost();
            if (scheme == null || host == null) {
                return url;
            }
            int port = u.getPort();
            if (port == -1) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
