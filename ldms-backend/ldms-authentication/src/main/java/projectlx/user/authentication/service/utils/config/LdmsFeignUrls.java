package projectlx.user.authentication.service.utils.config;

import org.springframework.core.env.Environment;

import java.net.URI;

/**
 * Resolves outbound Feign base URLs after the full environment is bound (including Config Server).
 * When {@code ldms.dev.force-local-feign-clients=true}, Docker hostnames from shared config are ignored.
 */
public final class LdmsFeignUrls {

    private LdmsFeignUrls() {}

    public static String resolveUserManagementServiceBaseUrl(Environment env) {
        String explicit = env.getProperty("CLIENTS_USER_MANAGEMENT_SERVICE_URL");
        if (explicit != null && !explicit.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(explicit.trim()));
        }
        if (Boolean.parseBoolean(env.getProperty("ldms.dev.force-local-feign-clients", "false"))) {
            return resolveLocalUserManagementBaseUrl(env);
        }
        String configured = env.getProperty("clients.base-url.userManagementService");
        if (configured != null && !configured.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(configured));
        }
        return resolveLocalUserManagementBaseUrl(env);
    }

    private static String resolveLocalUserManagementBaseUrl(Environment env) {
        String host = env.getProperty("USER_MANAGEMENT_HOST", "127.0.0.1");
        String port = env.getProperty("USER_MANAGEMENT_PORT", "8086");
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
