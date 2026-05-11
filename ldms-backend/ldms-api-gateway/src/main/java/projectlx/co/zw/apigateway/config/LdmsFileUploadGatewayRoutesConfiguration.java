package projectlx.co.zw.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers file-upload routes independently of {@code spring.cloud.gateway.routes} from Config Server.
 * <p>
 * When the central config replaces the entire route list, YAML routes in this JAR can disappear; these
 * programmatic routes keep {@code /ldms-file-upload-service/**} reachable through the gateway.
 */
@Configuration
@ConditionalOnProperty(name = "ldms.gateway.file-upload.routes-enabled", havingValue = "true", matchIfMissing = true)
public class LdmsFileUploadGatewayRoutesConfiguration {

    @Bean
    public RouteLocator ldmsFileUploadGatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${FILE_UPLOAD_HOST:127.0.0.1}") String fileUploadHost,
            @Value("${FILE_UPLOAD_PORT:8085}") int fileUploadPort) {
        String uri = "http://" + fileUploadHost + ":" + fileUploadPort;
        return builder.routes()
                .route(
                        "ldms-file-upload-service-system",
                        r -> r.path("/ldms-file-upload-service/v1/system/**").uri(uri))
                .route(
                        "ldms-file-upload-service-frontend",
                        r -> r.path("/ldms-file-upload-service/v1/frontend/**").uri(uri))
                .build();
    }
}
