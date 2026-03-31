package projectlx.co.zw.locationsmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {
        "projectlx.co.zw.locationsmanagementservice",
        "projectlx.co.zw.shared_library"})
public class LocationsManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocationsManagementServiceApplication.class, args);
    }

}
