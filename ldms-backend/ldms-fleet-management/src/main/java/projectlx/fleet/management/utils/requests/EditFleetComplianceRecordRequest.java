package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class EditFleetComplianceRecordRequest {
    private Long id;
    private Long fileUploadId;
    private LocalDateTime expiresAt;
    private String status;
    private String notes;
}
