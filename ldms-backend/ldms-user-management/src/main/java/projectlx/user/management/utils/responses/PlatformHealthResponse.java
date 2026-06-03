package projectlx.user.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.management.utils.dtos.EurekaHealthSummaryDto;
import projectlx.user.management.utils.dtos.InfrastructureHealthDto;
import projectlx.user.management.utils.dtos.PlatformHealthSummaryDto;
import projectlx.user.management.utils.dtos.ServiceHealthSnapshotDto;
import projectlx.user.management.utils.enums.PlatformOverallStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformHealthResponse extends CommonResponse {
    private LocalDateTime checkedAt;
    private PlatformOverallStatus overallStatus;
    private PlatformHealthSummaryDto summary;
    private List<ServiceHealthSnapshotDto> services = new ArrayList<>();
    private List<InfrastructureHealthDto> infrastructure = new ArrayList<>();
    private EurekaHealthSummaryDto eureka;
}
