package projectlx.co.zw.fleetmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class CreateFleetComplianceRecordRequest {
    private String subjectType;
    private Long subjectId;
    private String complianceType;
    private Long fileUploadId;
    private LocalDateTime expiresAt;
    private String notes;
}
