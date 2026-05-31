package projectlx.user.management.business.logic.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import projectlx.user.management.business.logic.api.PlatformHealthService;
import projectlx.user.management.utils.config.PlatformHealthProperties;
import projectlx.user.management.utils.dtos.EurekaHealthSummaryDto;
import projectlx.user.management.utils.dtos.InfrastructureHealthDto;
import projectlx.user.management.utils.dtos.PlatformHealthSummaryDto;
import projectlx.user.management.utils.dtos.ServiceHealthSnapshotDto;
import projectlx.user.management.utils.enums.PlatformOverallStatus;
import projectlx.user.management.utils.responses.PlatformHealthResponse;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

    public PlatformHealthServiceImpl(PlatformHealthProperties properties,
                                     ObjectProvider<DiscoveryClient> discoveryClientProvider,
                                     ObjectMapper objectMapper) {
        this.properties = properties;
        this.discoveryClientProvider = discoveryClientProvider;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(properties.getProbeTimeoutMs(), 1000);
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public PlatformHealthResponse snapshot(Locale locale) {
        log.info("Building platform health snapshot");

        List<PlatformHealthProperties.ServiceTarget> targets = properties.getServices();
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

    private ServiceHealthSnapshotDto probeService(PlatformHealthProperties.ServiceTarget target) {
        ServiceHealthSnapshotDto snapshot = new ServiceHealthSnapshotDto();
        snapshot.setServiceId(target.getId());
        snapshot.setDisplayName(target.getDisplayName());
        snapshot.setEurekaServiceName(target.getEurekaName());

        String host = resolveHost(target);
        boolean useManagementPort = target.getManagementPort() != null;
        int probePort = useManagementPort ? target.getManagementPort() : target.getPort();
        snapshot.setHost(host);
        snapshot.setPort(probePort);
        snapshot.setManagementPortUsed(useManagementPort);

        URI healthUri = URI.create("http://" + host + ":" + probePort + "/actuator/health");
        long startNanos = System.nanoTime();
        try {
            String body = restClient.get()
                    .uri(healthUri)
                    .retrieve()
                    .body(String.class);
            snapshot.setLatencyMs((System.nanoTime() - startNanos) / 1_000_000L);
            parseHealthBody(snapshot, body);
        } catch (Exception ex) {
            snapshot.setLatencyMs((System.nanoTime() - startNanos) / 1_000_000L);
            snapshot.setStatus("DOWN");
            snapshot.setMessage(ex.getMessage());
            log.debug("Health probe failed for {} at {}: {}", target.getId(), healthUri, ex.toString());
        }
        return snapshot;
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
            } else if ("UP".equalsIgnoreCase(status)) {
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
