package projectlx.co.zw.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import projectlx.co.zw.shared_library.utils.config.UtilsConfig;
import projectlx.co.zw.shared_library.utils.security.config.LdmsMethodSecurityConfiguration;
import projectlx.co.zw.shared_library.utils.security.config.SharedJwtSecurityConfig;

@SpringBootApplication(
		exclude = { UserDetailsServiceAutoConfiguration.class },
		scanBasePackages = {
				"projectlx.co.zw.notifications",
				"projectlx.co.zw.shared_library"
		})
@EnableMethodSecurity
@Import({ SharedJwtSecurityConfig.class, LdmsMethodSecurityConfiguration.class, UtilsConfig.class })
public class Notifications {

	public static void main(String[] args) {
		SpringApplication.run(Notifications.class, args);
	}

}
