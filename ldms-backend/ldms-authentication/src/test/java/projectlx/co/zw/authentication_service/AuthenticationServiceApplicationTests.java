package projectlx.co.zw.authentication_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import projectlx.user.authentication.service.AuthenticationServiceApplication;

@Disabled("Full context test depends on external logging/infrastructure during local builds")
@SpringBootTest(
		classes = AuthenticationServiceApplication.class,
		properties = {
				"spring.cloud.config.enabled=false",
				"eureka.client.enabled=false"
		}
)
class AuthenticationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
