package projectlx.user.authentication.service.service.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Project LX: (LDMS) Authentication Microservice")
                        .version("1.0")
                        .description("Project LX Spring Boot API documentation"));
    }
}
