package projectlx.fleet.management.config;

import org.springframework.core.env.Environment;

import java.net.URI;

public final class LdmsFeignUrls {

    private LdmsFeignUrls() {}

    public static String resolveOrganizationManagementServiceBaseUrl(Environment env) {
        String explicit = env.getProperty("CLIENTS_ORGANIZATION_MANAGEMENT_SERVICE_URL");
        if (explicit != null && !explicit.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(explicit.trim()));
        }
        if (Boolean.parseBoolean(env.getProperty("ldms.dev.force-local-feign-clients", "false"))) {
            return resolveApiGatewayBaseUrl(env);
        }
        String configured = env.getProperty("clients.base-url.organizationManagementService");
        if (configured != null && !configured.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(configured));
        }
        return resolveApiGatewayBaseUrl(env);
    }

    public static String resolveUserManagementServiceBaseUrl(Environment env) {
        String explicit = env.getProperty("CLIENTS_USER_MANAGEMENT_SERVICE_URL");
        if (explicit != null && !explicit.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(explicit.trim()));
        }
        if (Boolean.parseBoolean(env.getProperty("ldms.dev.force-local-feign-clients", "false"))) {
            return resolveApiGatewayBaseUrl(env);
        }
        String configured = env.getProperty("clients.base-url.userManagementService");
        if (configured != null && !configured.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(configured));
        }
        return resolveApiGatewayBaseUrl(env);
    }

    public static String resolveShipmentManagementServiceBaseUrl(Environment env) {
        String explicit = env.getProperty("CLIENTS_SHIPMENT_MANAGEMENT_SERVICE_URL");
        if (explicit != null && !explicit.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(explicit.trim()));
        }
        if (Boolean.parseBoolean(env.getProperty("ldms.dev.force-local-feign-clients", "false"))) {
            return resolveApiGatewayBaseUrl(env);
        }
        String configured = env.getProperty("clients.base-url.shipmentManagementService");
        if (configured != null && !configured.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(configured));
        }
        return resolveApiGatewayBaseUrl(env);
    }

    public static String resolveFileUploadServiceBaseUrl(Environment env) {
        String explicit = env.getProperty("CLIENTS_FILE_UPLOAD_SERVICE_URL");
        if (explicit != null && !explicit.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(explicit.trim()));
        }
        if (Boolean.parseBoolean(env.getProperty("ldms.dev.force-local-feign-clients", "false"))) {
            return resolveApiGatewayBaseUrl(env);
        }
        String configured = env.getProperty("clients.base-url.fileUploadService");
        if (configured != null && !configured.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(configured));
        }
        return resolveApiGatewayBaseUrl(env);
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
