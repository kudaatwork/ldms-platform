package projectlx.inventory.management.clients.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordPlatformUsageChargeRequest {
    private Long organizationId;
    private String actionCode;
    private Long tripId;
    private Long seasonId;
    private String referenceType;
    private Long referenceId;
    private String serviceName;
    private String traceId;
}
