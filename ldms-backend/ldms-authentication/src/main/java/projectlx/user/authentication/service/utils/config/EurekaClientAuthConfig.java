package projectlx.user.authentication.service.utils.config;

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.config.DiscoveryClientOptionalArgsConfiguration;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class EurekaClientAuthConfig {

    @Value("${eureka.username:${EUREKA_USERNAME:eureka}}")
    private String eurekaUsername;

    @Value("${eureka.password:${EUREKA_PASSWORD:Eureka098()!}}")
    private String eurekaPassword;

    @Bean
    public InitializingBean sanitizeEurekaServiceUrls(EurekaClientConfigBean eurekaClientConfigBean) {
        return () -> {
            Map<String, String> serviceUrls = eurekaClientConfigBean.getServiceUrl();
            if (serviceUrls == null || serviceUrls.isEmpty()) {
                return;
            }

            serviceUrls.replaceAll((zone, url) -> enrichServiceUrlListWithCredentials(url));
        };
    }

    @Bean
    @ConditionalOnMissingBean(AbstractDiscoveryClientOptionalArgs.class)
    public RestTemplateDiscoveryClientOptionalArgs restTemplateDiscoveryClientOptionalArgs(
            TlsProperties tlsProperties,
            EurekaClientHttpRequestFactorySupplier eurekaClientHttpRequestFactorySupplier)
            throws GeneralSecurityException, IOException {
        RestTemplateDiscoveryClientOptionalArgs args = new RestTemplateDiscoveryClientOptionalArgs(
                eurekaClientHttpRequestFactorySupplier,
                () -> new RestTemplateBuilder().basicAuthentication(eurekaUsername, eurekaPassword)
        );
        DiscoveryClientOptionalArgsConfiguration.setupTLS(args, tlsProperties);
        return args;
    }

    private String enrichServiceUrlListWithCredentials(String rawUrls) {
        if (rawUrls == null || rawUrls.isBlank()) {
            return rawUrls;
        }

        return Arrays.stream(rawUrls.split(","))
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .map(this::stripUserInfo)
                .map(this::injectUserInfo)
                .collect(Collectors.joining(","));
    }

    private String injectUserInfo(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String userInfo = urlEncode(eurekaUsername) + ":" + urlEncode(eurekaPassword);
            return new URI(
                    uri.getScheme(),
                    userInfo,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toASCIIString();
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            return rawUrl;
        }
    }

    private String stripUserInfo(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            if (uri.getUserInfo() == null) {
                return rawUrl;
            }

            return new URI(
                    uri.getScheme(),
                    null,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            return rawUrl.replaceFirst("://[^/@]+@", "://");
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
