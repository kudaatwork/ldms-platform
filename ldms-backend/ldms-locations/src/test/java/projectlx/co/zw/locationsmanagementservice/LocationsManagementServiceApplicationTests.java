package projectlx.co.zw.locationsmanagementservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Full context requires ldms-config-server (8888), MySQL, and RabbitMQ; dev profile imports Config Server by default.")
@SpringBootTest
class LocationsManagementServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
