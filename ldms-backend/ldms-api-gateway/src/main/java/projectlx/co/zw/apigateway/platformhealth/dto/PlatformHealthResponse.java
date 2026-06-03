package projectlx.co.zw.apigateway.platformhealth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.apigateway.platformhealth.PlatformOverallStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformHealthResponse {
    private int statusCode;
    private boolean success;
    private String message;
    private LocalDateTime checkedAt;
    private PlatformOverallStatus overallStatus;
    private PlatformHealthSummaryDto summary;
    private List<ServiceHealthSnapshotDto> services = new ArrayList<>();
    private List<InfrastructureHealthDto> infrastructure = new ArrayList<>();
    private EurekaHealthSummaryDto eureka;
}
