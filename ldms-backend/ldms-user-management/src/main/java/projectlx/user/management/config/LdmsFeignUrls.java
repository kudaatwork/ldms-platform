package projectlx.user.management.config;

import org.springframework.core.env.Environment;

import java.net.URI;

/**
 * Resolves outbound Feign base URLs after the full environment is bound (including Config Server).
 * Local IDE runs use {@code ldms.dev.force-local-feign-clients=true} (see profile-specific {@code application.yml})
 * so Docker-only hostnames from shared config are not used on the host OS.
 */
public final class LdmsFeignUrls {

    private LdmsFeignUrls() {}

    public static String resolveLocationServiceBaseUrl(Environment env) {
        String explicit = env.getProperty("CLIENTS_LOCATION_SERVICE_URL");
        if (explicit != null && !explicit.isBlank()) {
            return trimTrailingSlash(explicit.trim());
        }
        if (Boolean.parseBoolean(env.getProperty("ldms.dev.force-local-feign-clients", "false"))) {
            String host = env.getProperty("LOCATION_SERVICE_HOST", "127.0.0.1");
            String port = env.getProperty("LOCATION_MANAGEMENT_SERVER_PORT", "8084");
            return "http://" + host + ":" + port;
        }
        String configured = env.getProperty("clients.base-url.locationService", "http://127.0.0.1:8084");
        return trimTrailingSlash(configured);
    }

    /**
     * Host and port only (no path). Paths live on {@link projectlx.user.management.clients.FileUploadServiceClient}.
     * If {@code clients.base-url.fileUploadService} includes a path (legacy), it is stripped to the origin.
     */
    public static String resolveFileUploadServiceBaseUrl(Environment env) {
        String explicit = env.getProperty("CLIENTS_FILE_UPLOAD_SERVICE_URL");
        if (explicit != null && !explicit.isBlank()) {
            return extractHttpOrigin(trimTrailingSlash(explicit.trim()));
        }
        if (Boolean.parseBoolean(env.getProperty("ldms.dev.force-local-feign-clients", "false"))) {
            String host = env.getProperty("DOCUMENTS_SERVICE_HOST", "127.0.0.1");
            String port = env.getProperty("DOCUMENTS_FILE_UPLOAD_SERVER_PORT", "8085");
            return "http://" + host + ":" + port;
        }
        String configured = env.getProperty("clients.base-url.fileUploadService", "http://127.0.0.1:8085");
        return extractHttpOrigin(trimTrailingSlash(configured));
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
