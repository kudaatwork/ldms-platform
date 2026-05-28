package projectlx.co.zw.apigateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * Browser CORS is configured in {@code application.yml} under
 * {@code spring.cloud.gateway.globalcors} (with {@code add-to-simple-url-handler-mapping: true}
 * for OPTIONS preflight). Downstream {@code Access-Control-*} headers are stripped via default
 * filters in the same file.
 */
@Configuration
public class GatewayCorsConfiguration {
}
