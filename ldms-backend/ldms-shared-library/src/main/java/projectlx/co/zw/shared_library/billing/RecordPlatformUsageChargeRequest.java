package projectlx.co.zw.shared_library.billing;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
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
