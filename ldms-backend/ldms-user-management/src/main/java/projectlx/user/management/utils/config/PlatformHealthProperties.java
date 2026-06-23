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
        /** Extra Eureka registration names (e.g. ldms-fuel-expenses from config server). */
        private List<String> alternateEurekaNames = new ArrayList<>();
        private String host;
        private int port;
        private Integer managementPort;
        /** Fallback application ports when config-server and local overrides differ (e.g. 8092 vs 8017). */
        private List<Integer> alternatePorts = new ArrayList<>();
        /** Fallback management actuator ports (e.g. 9092 vs 9017). */
        private List<Integer> alternateManagementPorts = new ArrayList<>();
    }
}
