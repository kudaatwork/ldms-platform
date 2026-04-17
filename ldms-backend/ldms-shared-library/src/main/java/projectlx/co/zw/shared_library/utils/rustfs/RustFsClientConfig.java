package projectlx.co.zw.shared_library.utils.rustfs;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(name = "rustfs.enabled", havingValue = "true", matchIfMissing = false)
public class RustFsClientConfig {

    @Bean("rustFsRestTemplate")
    public RestTemplate rustFsRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RustFsClient rustFsClient(
            @Qualifier("rustFsRestTemplate") RestTemplate rustFsRestTemplate,
            @Value("${rustfs.base-url:http://localhost:8200}") String rustFsBaseUrl,
            @Value("${rustfs.internal-token:changeme}") String internalToken) {
        return new RustFsClientImpl(rustFsRestTemplate, rustFsBaseUrl, internalToken);
    }
}
