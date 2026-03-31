package projectlx.co.zw.notificationsmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"projectlx.co.zw.notificationsmanagementservice",
		"projectlx.co.zw.shared_library"})
public class NotificationsManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationsManagementServiceApplication.class, args);
	}

}
