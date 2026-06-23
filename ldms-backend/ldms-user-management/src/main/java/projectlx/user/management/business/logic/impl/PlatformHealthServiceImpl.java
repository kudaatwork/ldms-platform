package projectlx.user.management.business.logic.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import projectlx.user.management.business.logic.api.PlatformHealthService;
import projectlx.user.management.utils.config.PlatformHealthProperties;
import projectlx.user.management.utils.dtos.EurekaHealthSummaryDto;
import projectlx.user.management.utils.dtos.InfrastructureHealthDto;
import projectlx.user.management.utils.dtos.PlatformHealthSummaryDto;
import projectlx.user.management.utils.dtos.ServiceHealthSnapshotDto;
import projectlx.user.management.utils.enums.PlatformOverallStatus;
import projectlx.user.management.utils.responses.PlatformHealthResponse;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class PlatformHealthServiceImpl implements PlatformHealthService {

    private static final Set<String> INFRASTRUCTURE_COMPONENTS =
            Set.of("db", "redis", "rabbit", "diskSpace", "ping");

    private final PlatformHealthProperties properties;
    private final ObjectProvider<DiscoveryClient> discoveryClientProvider;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final int probeTimeoutMs;

    public PlatformHealthServiceImpl(PlatformHealthProperties properties,
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

    @Override
    public PlatformHealthResponse snapshot(Locale locale) {
        log.info("Building platform health snapshot");

        List<PlatformHealthProperties.ServiceTarget> targets = resolveProbeTargets();
        List<ServiceHealthSnapshotDto> serviceSnapshots = targets.parallelStream()
                .map(this::probeService)
                .sorted(Comparator.comparing(ServiceHealthSnapshotDto::getDisplayName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toCollection(ArrayList::new));

        PlatformHealthSummaryDto summary = buildSummary(serviceSnapshots);
        PlatformOverallStatus overallStatus = resolveOverallStatus(summary);
        List<InfrastructureHealthDto> infrastructure = aggregateInfrastructure(serviceSnapshots);
        EurekaHealthSummaryDto eureka = buildEurekaSummary();

        PlatformHealthResponse response = new PlatformHealthResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Platform health snapshot");
        response.setCheckedAt(LocalDateTime.now());
        response.setOverallStatus(overallStatus);
        response.setSummary(summary);
        response.setServices(serviceSnapshots);
        response.setInfrastructure(infrastructure);
        response.setEureka(eureka);
        return response;
    }

    /**
     * Merges statically configured probe targets with every Eureka-registered application that is not
     * already covered by a configured {@code eureka-name}, so the admin health dashboard reflects the
     * full running fleet (not only the hard-coded subset).
     */
    private List<PlatformHealthProperties.ServiceTarget> resolveProbeTargets() {
        List<PlatformHealthProperties.ServiceTarget> targets = new ArrayList<>(properties.getServices());
        if (!properties.isAutoDiscoverEurekaServices()) {
            return targets;
        }

        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient == null) {
            return targets;
        }

        Set<String> monitoredEurekaNames = new LinkedHashSet<>();
        for (PlatformHealthProperties.ServiceTarget target : targets) {
            for (String eurekaName : eurekaNamesFor(target)) {
                for (ServiceInstance instance : lookupEurekaInstances(discoveryClient, eurekaName)) {
                    if (instance.getServiceId() != null && !instance.getServiceId().isBlank()) {
                        monitoredEurekaNames.add(instance.getServiceId().toLowerCase(Locale.ROOT));
                    }
                }
                monitoredEurekaNames.add(eurekaName.toLowerCase(Locale.ROOT));
            }
        }

        for (String serviceName : discoveryClient.getServices()) {
            if (serviceName == null || serviceName.isBlank()) {
                continue;
            }
            if (monitoredEurekaNames.contains(serviceName.toLowerCase(Locale.ROOT))) {
                continue;
            }
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances.isEmpty()) {
                continue;
            }
            ServiceInstance primary = instances.get(0);
            PlatformHealthProperties.ServiceTarget dynamic = new PlatformHealthProperties.ServiceTarget();
            dynamic.setId(sanitizeServiceId(serviceName));
            dynamic.setDisplayName(formatEurekaDisplayName(serviceName));
            dynamic.setEurekaName(serviceName);
            dynamic.setHost(primary.getHost());
            dynamic.setPort(primary.getPort());
            String managementPort = primary.getMetadata().get("management.port");
            if (managementPort != null && !managementPort.isBlank()) {
                try {
                    dynamic.setManagementPort(Integer.parseInt(managementPort.trim()));
                } catch (NumberFormatException ex) {
                    log.debug("Invalid management.port metadata for {}: {}", serviceName, managementPort);
                }
            }
            targets.add(dynamic);
            monitoredEurekaNames.add(serviceName.toLowerCase(Locale.ROOT));
        }
        return targets;
    }

    private String sanitizeServiceId(String eurekaName) {
        String normalized = eurekaName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return normalized.replaceAll("^-+|-+$", "");
    }

    private String formatEurekaDisplayName(String eurekaName) {
        String label = eurekaName.trim();
        if (label.toLowerCase(Locale.ROOT).startsWith("ldms-")) {
            label = label.substring(5);
        }
        if (label.toLowerCase(Locale.ROOT).endsWith("-service")) {
            label = label.substring(0, label.length() - "-service".length());
        }
        String[] parts = label.split("-");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return !builder.isEmpty() ? builder.toString() : eurekaName;
    }

    private ServiceHealthSnapshotDto probeService(PlatformHealthProperties.ServiceTarget target) {
        ServiceHealthSnapshotDto snapshot = new ServiceHealthSnapshotDto();
        snapshot.setServiceId(target.getId());
        snapshot.setDisplayName(target.getDisplayName());
        snapshot.setEurekaServiceName(target.getEurekaName());

        String configuredHost = resolveHost(target);

        int registeredInstances = countEurekaInstances(target);
        if (registeredInstances > 0) {
            snapshot.setHost(configuredHost);
            snapshot.setPort(resolveApplicationPort(target));
            snapshot.setManagementPortUsed(target.getManagementPort() != null);
            snapshot.setStatus("UP");
            long tcpStart = System.nanoTime();
            boolean reachable = isApplicationPortReachable(target, configuredHost);
            snapshot.setLatencyMs((System.nanoTime() - tcpStart) / 1_000_000L);
            snapshot.setMessage(reachable
                    ? "Registered in Eureka (" + registeredInstances + " instance"
                    + (registeredInstances == 1 ? "" : "s") + "); process reachable on port " + resolveApplicationPort(target)
                    : "Registered in Eureka (" + registeredInstances + " instance"
                    + (registeredInstances == 1 ? "" : "s") + "); actuator health endpoint unavailable");
            return snapshot;
        }

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
                if (markUpWhenApplicationPortReachable(snapshot, target, configuredHost)) {
                    return snapshot;
                }
            } catch (Exception ex) {
                snapshot.setLatencyMs((System.nanoTime() - startNanos) / 1_000_000L);
                lastFailure = ex;
                log.debug("Health probe failed for {} at {}: {}", target.getId(), healthUri, ex.toString());
            }
        }

        int eurekaInstances = countEurekaInstances(target);
        if (eurekaInstances > 0) {
            snapshot.setHost(configuredHost);
            snapshot.setPort(resolveApplicationPort(target));
            snapshot.setManagementPortUsed(target.getManagementPort() != null);
            snapshot.setStatus("UP");
            snapshot.setMessage("Registered in Eureka (" + eurekaInstances + " instance"
                    + (eurekaInstances == 1 ? "" : "s")
                    + "); actuator health endpoint unavailable");
            return snapshot;
        }

        if (isApplicationPortReachable(target, configuredHost)) {
            int reachablePort = resolveReachableApplicationPort(target, configuredHost);
            snapshot.setHost(configuredHost);
            snapshot.setPort(reachablePort);
            snapshot.setManagementPortUsed(false);
            snapshot.setStatus("UP");
            snapshot.setMessage("Process reachable on port " + reachablePort
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

        for (int port : managementPortsFor(target)) {
            for (String host : probeHostsFor(target, configuredHost)) {
                addProbeTarget(targets, seen, host, port, true);
            }
        }
        for (int port : applicationPortsFor(target)) {
            for (String host : probeHostsFor(target, configuredHost)) {
                addProbeTarget(targets, seen, host, port, false);
            }
        }

        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient != null) {
            for (ServiceInstance instance : resolveEurekaInstances(discoveryClient, target)) {
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

    private List<ServiceInstance> resolveEurekaInstances(DiscoveryClient discoveryClient,
                                                       PlatformHealthProperties.ServiceTarget target) {
        LinkedHashSet<String> seenInstanceIds = new LinkedHashSet<>();
        List<ServiceInstance> merged = new ArrayList<>();
        for (String eurekaName : eurekaNamesFor(target)) {
            List<ServiceInstance> instances = lookupEurekaInstances(discoveryClient, eurekaName);
            for (ServiceInstance instance : instances) {
                String key = instance.getInstanceId() != null
                        ? instance.getInstanceId()
                        : instance.getHost() + ":" + instance.getPort();
                if (seenInstanceIds.add(key)) {
                    merged.add(instance);
                }
            }
        }
        return merged;
    }

    private List<ServiceInstance> lookupEurekaInstances(DiscoveryClient discoveryClient, String eurekaName) {
        if (eurekaName == null || eurekaName.isBlank()) {
            return List.of();
        }
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

    private List<String> eurekaNamesFor(PlatformHealthProperties.ServiceTarget target) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (target.getEurekaName() != null && !target.getEurekaName().isBlank()) {
            names.add(target.getEurekaName().trim());
        }
        if (target.getAlternateEurekaNames() != null) {
            for (String alternate : target.getAlternateEurekaNames()) {
                if (alternate != null && !alternate.isBlank()) {
                    names.add(alternate.trim());
                }
            }
        }
        return new ArrayList<>(names);
    }

    private int countEurekaInstances(PlatformHealthProperties.ServiceTarget target) {
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient == null) {
            return 0;
        }
        return resolveEurekaInstances(discoveryClient, target).size();
    }

    private int resolveApplicationPort(PlatformHealthProperties.ServiceTarget target) {
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient != null) {
            List<ServiceInstance> instances = resolveEurekaInstances(discoveryClient, target);
            if (!instances.isEmpty() && instances.get(0).getPort() > 0) {
                return instances.get(0).getPort();
            }
        }
        List<Integer> ports = applicationPortsFor(target);
        return ports.isEmpty() ? target.getPort() : ports.get(0);
    }

    private int resolveReachableApplicationPort(PlatformHealthProperties.ServiceTarget target, String configuredHost) {
        for (int port : applicationPortsFor(target)) {
            if (isPortReachableOnAnyHost(target, configuredHost, port)) {
                return port;
            }
        }
        return resolveApplicationPort(target);
    }

    private List<Integer> applicationPortsFor(PlatformHealthProperties.ServiceTarget target) {
        LinkedHashSet<Integer> ports = new LinkedHashSet<>();
        addPort(ports, target.getPort());
        if (target.getAlternatePorts() != null) {
            for (Integer port : target.getAlternatePorts()) {
                addPort(ports, port);
            }
        }
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient != null) {
            for (ServiceInstance instance : resolveEurekaInstances(discoveryClient, target)) {
                addPort(ports, instance.getPort());
            }
        }
        return new ArrayList<>(ports);
    }

    private List<Integer> managementPortsFor(PlatformHealthProperties.ServiceTarget target) {
        LinkedHashSet<Integer> ports = new LinkedHashSet<>();
        addPort(ports, target.getManagementPort());
        if (target.getAlternateManagementPorts() != null) {
            for (Integer port : target.getAlternateManagementPorts()) {
                addPort(ports, port);
            }
        }
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient != null) {
            for (ServiceInstance instance : resolveEurekaInstances(discoveryClient, target)) {
                String managementPort = instance.getMetadata().get("management.port");
                if (managementPort != null && !managementPort.isBlank()) {
                    try {
                        addPort(ports, Integer.parseInt(managementPort.trim()));
                    } catch (NumberFormatException ignored) {
                        log.debug("Invalid management.port metadata for {}: {}", target.getId(), managementPort);
                    }
                }
            }
        }
        return new ArrayList<>(ports);
    }

    private void addPort(LinkedHashSet<Integer> ports, Integer port) {
        if (port != null && port > 0) {
            ports.add(port);
        }
    }

    private LinkedHashSet<String> probeHostsFor(PlatformHealthProperties.ServiceTarget target, String configuredHost) {
        LinkedHashSet<String> hosts = new LinkedHashSet<>();
        if (configuredHost != null && !configuredHost.isBlank()) {
            hosts.add(configuredHost.trim());
        }
        hosts.add("127.0.0.1");
        hosts.add("localhost");

        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient != null) {
            for (ServiceInstance instance : resolveEurekaInstances(discoveryClient, target)) {
                if (instance.getHost() != null && !instance.getHost().isBlank()) {
                    hosts.add(instance.getHost().trim());
                }
            }
        }
        return hosts;
    }

    private boolean markUpWhenApplicationPortReachable(ServiceHealthSnapshotDto snapshot,
                                                       PlatformHealthProperties.ServiceTarget target,
                                                       String configuredHost) {
        if (!isApplicationPortReachable(target, configuredHost)) {
            return false;
        }
        String reported = snapshot.getStatus();
        snapshot.setHost(configuredHost);
        snapshot.setPort(resolveReachableApplicationPort(target, configuredHost));
        snapshot.setManagementPortUsed(false);
        snapshot.setStatus("UP");
        snapshot.setMessage("Process reachable on port " + resolveReachableApplicationPort(target, configuredHost)
                + (reported != null && !"UP".equalsIgnoreCase(reported)
                ? " (actuator aggregate reported " + reported + ")"
                : " (actuator health unavailable)"));
        return true;
    }

    private boolean isApplicationPortReachable(PlatformHealthProperties.ServiceTarget target, String configuredHost) {
        for (int port : applicationPortsFor(target)) {
            if (isPortReachableOnAnyHost(target, configuredHost, port)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPortReachableOnAnyHost(PlatformHealthProperties.ServiceTarget target,
                                           String configuredHost,
                                           int port) {
        for (String host : probeHostsFor(target, configuredHost)) {
            if (isTcpReachable(host, port)) {
                return true;
            }
        }
        return false;
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
            return;
        }
        String liveness = snapshot.getComponents().get("livenessState");
        if ("UP".equalsIgnoreCase(liveness)) {
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

    private String sanitizeProbeFailure(Exception ex, URI healthUri) {
        if (ex instanceof RestClientResponseException responseEx) {
            int status = responseEx.getStatusCode().value();
            String body = responseEx.getResponseBodyAsString();
            if (status == 404 || (body != null && body.toLowerCase().contains("whitelabel error"))) {
                return "Actuator /actuator/health not exposed on "
                        + healthUri.getHost() + ":" + healthUri.getPort() + " (HTTP 404)";
            }
        }
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Probe failed for " + healthUri;
        }
        return message.length() > 280 ? message.substring(0, 280) + "…" : message;
    }

    private record ProbeTarget(String host, int port, boolean managementPort) {}

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
}
