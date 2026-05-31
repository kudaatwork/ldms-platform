package projectlx.user.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class ServiceHealthSnapshotDto {
    private String serviceId;
    private String displayName;
    private String eurekaServiceName;
    private String host;
    private int port;
    private boolean managementPortUsed;
    private String status;
    private long latencyMs;
    private String message;
    private Map<String, String> components = new LinkedHashMap<>();
}
