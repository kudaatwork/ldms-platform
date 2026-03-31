package projectlx.co.zw.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"projectlx.co.zw.notifications",
		"projectlx.co.zw.shared_library"})
public class Notifications {

	public static void main(String[] args) {
		SpringApplication.run(Notifications.class, args);
	}

}
