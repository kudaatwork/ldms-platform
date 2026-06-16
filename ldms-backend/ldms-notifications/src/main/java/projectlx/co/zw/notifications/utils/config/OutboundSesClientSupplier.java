package projectlx.co.zw.notifications.utils.config;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * Builds an AWS SES client from {@code ldms-config-repo/.env} and Spring configuration.
 * Uses the regional HTTPS endpoint directly and ignores JVM HTTP proxy settings so corporate
 * proxies (e.g. {@code 192.168.x.x:80}) do not break SES calls on developer machines.
 */
@Component
public class OutboundSesClientSupplier {

    private static final Logger log = LoggerFactory.getLogger(OutboundSesClientSupplier.class);
    private static final String DEFAULT_REGION = "eu-west-1";

    private final LdmsConfigRepoSecretsResolver secretsResolver;
    private volatile SesClient sesClient;

    public OutboundSesClientSupplier(LdmsConfigRepoSecretsResolver secretsResolver) {
        this.secretsResolver = secretsResolver;
    }

    public boolean isConfigured() {
        return secretsResolver.isSesConfigured();
    }

    public SesClient getIfAvailable() {
        if (!isConfigured()) {
            return null;
        }
        if (sesClient != null) {
            return sesClient;
        }
        synchronized (this) {
            if (sesClient != null) {
                return sesClient;
            }
            try {
                sesClient = buildClient();
                log.info("[NOTIFICATION] AWS SES client initialized (region={}, endpoint={})",
                        resolveRegion(), sesEndpointUri(resolveRegion()));
            } catch (Exception ex) {
                log.error("[NOTIFICATION] Failed to initialize AWS SES client: {}", ex.getMessage());
                return null;
            }
        }
        return sesClient;
    }

    public String fromEmail() {
        return secretsResolver.awsSesFromEmail();
    }

    private SesClient buildClient() {
        String region = resolveRegion();
        SdkHttpClient httpClient = ApacheHttpClient.builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                        .useSystemPropertyValues(false)
                        .build())
                .build();

        return SesClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(sesEndpointUri(region)))
                .credentialsProvider(credentialsProvider())
                .httpClient(httpClient)
                .build();
    }

    private static String sesEndpointUri(String region) {
        return "https://email." + region + ".amazonaws.com";
    }

    private AwsCredentialsProvider credentialsProvider() {
        String accessKey = secretsResolver.awsAccessKeyId();
        String secretKey = secretsResolver.awsSecretAccessKey();
        if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey.trim(), secretKey.trim()));
        }
        return DefaultCredentialsProvider.create();
    }

    private String resolveRegion() {
        String region = secretsResolver.awsRegion();
        return StringUtils.hasText(region) ? region.trim() : DEFAULT_REGION;
    }
}
