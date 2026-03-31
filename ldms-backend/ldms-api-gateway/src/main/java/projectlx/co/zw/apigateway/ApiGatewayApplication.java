package projectlx.co.zw.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import projectlx.co.zw.apigateway.config.GatewayJwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
