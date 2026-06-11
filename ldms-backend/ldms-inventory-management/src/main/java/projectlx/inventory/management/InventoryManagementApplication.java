package projectlx.inventory.management;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import projectlx.co.zw.shared_library.utils.config.UtilsConfig;
import projectlx.co.zw.shared_library.utils.security.config.LdmsMethodSecurityConfiguration;
import projectlx.co.zw.shared_library.utils.security.config.SharedJwtSecurityConfig;

@EnableRetry
@EnableFeignClients
@EnableDiscoveryClient
@EnableMethodSecurity
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class }, scanBasePackages = {
        "projectlx.inventory.management",
        "projectlx.co.zw.shared_library"
})
@Import({ SharedJwtSecurityConfig.class, LdmsMethodSecurityConfiguration.class, UtilsConfig.class })
public class InventoryManagementApplication {

    private static final String PORT_PROPERTY_SOURCE = "inventoryManagementLocalPort";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(InventoryManagementApplication.class);
        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event ->
                applyLocalPort(event.getEnvironment()));
        app.run(args);
    }

    private static void applyLocalPort(ConfigurableEnvironment environment) {
        String explicitPort = environment.getProperty("INVENTORY_MANAGEMENT_PORT");
        if (explicitPort != null && !explicitPort.isBlank()) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(
                PORT_PROPERTY_SOURCE,
                Map.of(
                        "server.port", "8013",
                        "management.server.port", "9013")));
    }
}
