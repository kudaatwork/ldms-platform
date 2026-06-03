package projectlx.co.zw.apigateway.platformhealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import projectlx.co.zw.apigateway.platformhealth.config.PlatformHealthProperties;
import projectlx.co.zw.apigateway.platformhealth.dto.EurekaHealthSummaryDto;
import projectlx.co.zw.apigateway.platformhealth.dto.InfrastructureHealthDto;
import projectlx.co.zw.apigateway.platformhealth.dto.PlatformHealthResponse;
import projectlx.co.zw.apigateway.platformhealth.dto.PlatformHealthSummaryDto;
import projectlx.co.zw.apigateway.platformhealth.dto.ServiceHealthSnapshotDto;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlatformHealthAggregator {

    private static final String API_GATEWAY_SERVICE_ID = "api-gateway";

    private static final Set<String> INFRASTRUCTURE_COMPONENTS =
            Set.of("db", "redis", "rabbit", "diskSpace", "ping");

    private final PlatformHealthProperties properties;
    private final ObjectProvider<DiscoveryClient> discoveryClientProvider;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final int probeTimeoutMs;

    public PlatformHealthAggregator(PlatformHealthProperties properties,
                                    ObjectProvider<DiscoveryClient> discoveryClientProvider,
                                    ObjectMapper objectMapper) {
        this.properties = properties;
        this.discoveryClientProvider = discoveryClientProvider;
        this.objectMapper = objectMapper;
        this.probeTimeoutMs = Math.max(properties.getProbeTimeoutMs(), 1000);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(probeTimeoutMs);
        requestFactory.setReadTimeout(probeTimeoutMs);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public PlatformHealthResponse snapshot() {
        log.info("Building platform health snapshot from API gateway");

        List<ServiceHealthSnapshotDto> serviceSnapshots = properties.getServices().parallelStream()
                .map(this::probeService)
                .sorted(Comparator.comparing(ServiceHealthSnapshotDto::getDisplayName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toCollection(ArrayList::new));

        PlatformHealthSummaryDto summary = buildSummary(serviceSnapshots);
        PlatformOverallStatus overallStatus = resolveOverallStatus(summary);

        PlatformHealthResponse response = new PlatformHealthResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Platform health snapshot");
        response.setCheckedAt(LocalDateTime.now());
        response.setOverallStatus(overallStatus);
        response.setSummary(summary);
        response.setServices(serviceSnapshots);
        response.setInfrastructure(aggregateInfrastructure(serviceSnapshots));
        response.setEureka(buildEurekaSummary());
        return response;
    }

    private ServiceHealthSnapshotDto probeService(PlatformHealthProperties.ServiceTarget target) {
        ServiceHealthSnapshotDto snapshot = new ServiceHealthSnapshotDto();
        snapshot.setServiceId(target.getId());
        snapshot.setDisplayName(target.getDisplayName());
        snapshot.setEurekaServiceName(target.getEurekaName());

        if (API_GATEWAY_SERVICE_ID.equalsIgnoreCase(target.getId())) {
            snapshot.setHost(resolveHost(target));
            snapshot.setPort(target.getPort());
            snapshot.setManagementPortUsed(false);
            snapshot.setStatus("UP");
            snapshot.setLatencyMs(0L);
            snapshot.setMessage("Running (in-process API gateway)");
            return snapshot;
        }

        String configuredHost = resolveHost(target);
        List<ProbeTarget> probeTargets = buildProbeTargets(target, configuredHost);
        Exception lastFailure = null;
        URI lastHealthUri = null;

        for (ProbeTarget probe : probeTargets) {
            snapshot.setHost(probe.host());
            snapshot.setPort(probe.port());
            snapshot.setManagementPortUsed(probe.managementPort());

            URI healthUri = URI.create("http://" + probe.host() + ":" + probe.port() + "/actuator/health");
            lastHealthUri = healthUri;
            long startNanos = System.nanoTime();
            try {
                String body = restClient.get()
                        .uri(healthUri)
                        .retrieve()
                        .body(String.class);
                snapshot.setLatencyMs((System.nanoTime() - startNanos) / 1_000_000L);
                parseHealthBody(snapshot, body);
                normalizeActuatorAggregateStatus(snapshot);
                if (isHealthyEnough(snapshot.getStatus())) {
                    return snapshot;
                }
            } catch (Exception ex) {
                snapshot.setLatencyMs((System.nanoTime() - startNanos) / 1_000_000L);
                lastFailure = ex;
                log.debug("Health probe failed for {} at {}: {}", target.getId(), healthUri, ex.toString());
            }
        }

        int eurekaInstances = countEurekaInstances(target.getEurekaName());
        if (eurekaInstances > 0) {
            snapshot.setHost(configuredHost);
            snapshot.setPort(target.getPort());
            snapshot.setManagementPortUsed(target.getManagementPort() != null);
            snapshot.setStatus("UP");
            snapshot.setMessage("Registered in Eureka (" + eurekaInstances + " instance"
                    + (eurekaInstances == 1 ? "" : "s")
                    + "); actuator health endpoint unavailable");
            return snapshot;
        }

        if (isTcpReachable(configuredHost, target.getPort())) {
            snapshot.setHost(configuredHost);
            snapshot.setPort(target.getPort());
            snapshot.setManagementPortUsed(false);
            snapshot.setStatus("UP");
            snapshot.setMessage("Process reachable on port " + target.getPort()
                    + " (actuator health unavailable)");
            return snapshot;
        }

        snapshot.setStatus("DOWN");
        snapshot.setMessage(lastFailure != null && lastHealthUri != null
                ? sanitizeProbeFailure(lastFailure, lastHealthUri)
                : "Service unreachable");
        return snapshot;
    }

    private List<ProbeTarget> buildProbeTargets(PlatformHealthProperties.ServiceTarget target, String configuredHost) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<ProbeTarget> targets = new ArrayList<>();

        if (target.getManagementPort() != null) {
            addProbeTarget(targets, seen, configuredHost, target.getManagementPort(), true);
        }
        addProbeTarget(targets, seen, configuredHost, target.getPort(), false);

        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient != null && target.getEurekaName() != null && !target.getEurekaName().isBlank()) {
            for (ServiceInstance instance : resolveEurekaInstances(discoveryClient, target.getEurekaName())) {
                addProbeTarget(targets, seen, instance.getHost(), instance.getPort(), false);
                String managementPort = instance.getMetadata().get("management.port");
                if (managementPort != null && !managementPort.isBlank()) {
                    try {
                        int port = Integer.parseInt(managementPort.trim());
                        addProbeTarget(targets, seen, instance.getHost(), port, true);
                    } catch (NumberFormatException ignored) {
                        log.debug("Invalid management.port metadata for {}: {}", target.getId(), managementPort);
                    }
                }
            }
        }
        return targets;
    }

    private void addProbeTarget(List<ProbeTarget> targets,
                                LinkedHashSet<String> seen,
                                String host,
                                int port,
                                boolean managementPort) {
        if (host == null || host.isBlank() || port <= 0) {
            return;
        }
        String normalizedHost = host.trim();
        String key = normalizedHost + ":" + port;
        if (seen.add(key)) {
            targets.add(new ProbeTarget(normalizedHost, port, managementPort));
        }
    }

    private List<ServiceInstance> resolveEurekaInstances(DiscoveryClient discoveryClient, String eurekaName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(eurekaName);
        if (!instances.isEmpty()) {
            return instances;
        }
        for (String registeredName : discoveryClient.getServices()) {
            if (registeredName.equalsIgnoreCase(eurekaName)) {
                return discoveryClient.getInstances(registeredName);
            }
        }
        return List.of();
    }

    private int countEurekaInstances(String eurekaName) {
        if (eurekaName == null || eurekaName.isBlank()) {
            return 0;
        }
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient == null) {
            return 0;
        }
        return resolveEurekaInstances(discoveryClient, eurekaName).size();
    }

    private boolean isTcpReachable(String host, int port) {
        if (host == null || host.isBlank() || port <= 0) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host.trim(), port), probeTimeoutMs);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private void parseHealthBody(ServiceHealthSnapshotDto snapshot, String body) {
        if (body == null || body.isBlank()) {
            snapshot.setStatus("UNKNOWN");
            snapshot.setMessage("Empty health response");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            snapshot.setStatus(root.path("status").asText("UNKNOWN"));
            JsonNode components = root.path("components");
            if (components.isObject()) {
                components.fields().forEachRemaining(entry -> {
                    String componentStatus = entry.getValue().path("status").asText("UNKNOWN");
                    snapshot.getComponents().put(entry.getKey(), componentStatus);
                });
            }
        } catch (Exception ex) {
            snapshot.setStatus("UNKNOWN");
            snapshot.setMessage("Failed to parse health JSON: " + ex.getMessage());
        }
    }

    /**
     * Spring Boot may report aggregate DOWN when optional contributors fail even though the JVM is up.
     */
    private void normalizeActuatorAggregateStatus(ServiceHealthSnapshotDto snapshot) {
        String status = snapshot.getStatus();
        if (status == null) {
            return;
        }
        if (!"DOWN".equalsIgnoreCase(status) && !"OUT_OF_SERVICE".equalsIgnoreCase(status)) {
            return;
        }
        String ping = snapshot.getComponents().get("ping");
        if ("UP".equalsIgnoreCase(ping)) {
            snapshot.setStatus("UP");
            snapshot.setMessage("Process up; some health contributors reported down");
        }
    }

    private boolean isHealthyEnough(String status) {
        if (status == null) {
            return false;
        }
        return "UP".equalsIgnoreCase(status) || "DEGRADED".equalsIgnoreCase(status);
    }

    private String resolveHost(PlatformHealthProperties.ServiceTarget target) {
        if (target.getHost() != null && !target.getHost().isBlank()) {
            return target.getHost().trim();
        }
        return properties.getDefaultHost();
    }

    private PlatformHealthSummaryDto buildSummary(List<ServiceHealthSnapshotDto> services) {
        PlatformHealthSummaryDto summary = new PlatformHealthSummaryDto();
        summary.setTotalServices(services.size());
        int up = 0;
        int down = 0;
        int unknown = 0;
        for (ServiceHealthSnapshotDto service : services) {
            String status = service.getStatus();
            if (status == null) {
                unknown++;
            } else if ("UP".equalsIgnoreCase(status) || "DEGRADED".equalsIgnoreCase(status)) {
                up++;
            } else if ("DOWN".equalsIgnoreCase(status)) {
                down++;
            } else {
                unknown++;
            }
        }
        summary.setUpCount(up);
        summary.setDownCount(down);
        summary.setUnknownCount(unknown);
        return summary;
    }

    private PlatformOverallStatus resolveOverallStatus(PlatformHealthSummaryDto summary) {
        if (summary.getTotalServices() == 0) {
            return PlatformOverallStatus.OUTAGE;
        }
        if (summary.getUpCount() == summary.getTotalServices()) {
            return PlatformOverallStatus.OPERATIONAL;
        }
        if (summary.getUpCount() == 0) {
            return PlatformOverallStatus.OUTAGE;
        }
        return PlatformOverallStatus.DEGRADED;
    }

    private List<InfrastructureHealthDto> aggregateInfrastructure(List<ServiceHealthSnapshotDto> services) {
        Map<String, List<String>> statusesByComponent = new LinkedHashMap<>();
        for (String component : INFRASTRUCTURE_COMPONENTS) {
            statusesByComponent.put(component, new ArrayList<>());
        }

        for (ServiceHealthSnapshotDto service : services) {
            if (service.getComponents() == null) {
                continue;
            }
            for (String component : INFRASTRUCTURE_COMPONENTS) {
                String status = service.getComponents().get(component);
                if (status != null) {
                    statusesByComponent.get(component).add(status);
                }
            }
        }

        List<InfrastructureHealthDto> aggregated = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : statusesByComponent.entrySet()) {
            List<String> statuses = entry.getValue();
            if (statuses.isEmpty()) {
                continue;
            }
            InfrastructureHealthDto infra = new InfrastructureHealthDto();
            infra.setComponent(entry.getKey());
            infra.setServicesReporting(statuses.size());
            int up = (int) statuses.stream().filter(s -> "UP".equalsIgnoreCase(s)).count();
            int down = (int) statuses.stream().filter(s -> "DOWN".equalsIgnoreCase(s)).count();
            infra.setServicesUp(up);
            infra.setServicesDown(down);
            infra.setStatus(resolveComponentStatus(up, down, statuses.size()));
            aggregated.add(infra);
        }
        return aggregated;
    }

    private String resolveComponentStatus(int up, int down, int reporting) {
        if (reporting == 0) {
            return "UNKNOWN";
        }
        if (up == reporting) {
            return "UP";
        }
        if (down == reporting) {
            return "DOWN";
        }
        return "DEGRADED";
    }

    private EurekaHealthSummaryDto buildEurekaSummary() {
        EurekaHealthSummaryDto summary = new EurekaHealthSummaryDto();
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient == null) {
            summary.setAvailable(false);
            return summary;
        }
        try {
            List<String> serviceNames = discoveryClient.getServices();
            summary.setAvailable(true);
            summary.setRegisteredServiceCount(serviceNames.size());
            int instanceCount = 0;
            List<EurekaHealthSummaryDto.EurekaServiceEntryDto> entries = new ArrayList<>();
            for (String serviceName : serviceNames) {
                int instances = discoveryClient.getInstances(serviceName).size();
                instanceCount += instances;
                EurekaHealthSummaryDto.EurekaServiceEntryDto entry =
                        new EurekaHealthSummaryDto.EurekaServiceEntryDto();
                entry.setServiceName(serviceName);
                entry.setInstanceCount(instances);
                entries.add(entry);
            }
            entries.sort(Comparator.comparing(EurekaHealthSummaryDto.EurekaServiceEntryDto::getServiceName,
                    String::compareToIgnoreCase));
            summary.setInstanceCount(instanceCount);
            summary.setServices(entries);
        } catch (Exception ex) {
            log.warn("Failed to read Eureka discovery summary: {}", ex.getMessage());
            summary.setAvailable(false);
        }
        return summary;
    }

    private String sanitizeProbeFailure(Exception ex, URI healthUri) {
        if (ex instanceof RestClientResponseException responseEx) {
            int status = responseEx.getStatusCode().value();
            String body = responseEx.getResponseBodyAsString();
            if (status == 404 || (body != null && body.toLowerCase().contains("whitelabel error"))) {
                return "Actuator /actuator/health not exposed on "
                        + healthUri.getHost() + ":" + healthUri.getPort()
                        + " (HTTP 404). Restart the service after enabling management.endpoints.web.exposure.";
            }
            if (body != null && !body.isBlank() && !body.startsWith("{") && !body.startsWith("[")) {
                return "HTTP " + status + " from " + healthUri.getHost() + ":" + healthUri.getPort();
            }
        }
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Probe failed for " + healthUri;
        }
        if (message.contains("<html") || message.toLowerCase().contains("whitelabel error")) {
            return "Actuator /actuator/health not available on "
                    + healthUri.getHost() + ":" + healthUri.getPort() + " (HTTP error page returned).";
        }
        return message.length() > 280 ? message.substring(0, 280) + "…" : message;
    }

    private record ProbeTarget(String host, int port, boolean managementPort) {}
}
