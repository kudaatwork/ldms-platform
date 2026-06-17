package projectlx.user.management.utils.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "ldms.platform-health")
public class PlatformHealthProperties {

    private int probeTimeoutMs = 5000;
    private String defaultHost = "127.0.0.1";
    /** When true, every Eureka-registered app not already in {@link #services} is probed automatically. */
    private boolean autoDiscoverEurekaServices = true;
    private List<ServiceTarget> services = new ArrayList<>();

    @Getter
    @Setter
    public static class ServiceTarget {
        private String id;
        private String displayName;
        private String eurekaName;
        private String host;
        private int port;
        private Integer managementPort;
    }
}
