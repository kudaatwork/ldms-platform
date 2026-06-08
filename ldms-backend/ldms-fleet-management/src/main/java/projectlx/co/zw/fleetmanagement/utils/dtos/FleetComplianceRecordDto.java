package projectlx.co.zw.fleetmanagement.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FleetComplianceRecordDto {
    private Long id;
    private Long organizationId;
    private String subjectType;
    private Long subjectId;
    private String complianceType;
    private Long fileUploadId;
    private LocalDateTime expiresAt;
    private String status;
    private String notes;
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
