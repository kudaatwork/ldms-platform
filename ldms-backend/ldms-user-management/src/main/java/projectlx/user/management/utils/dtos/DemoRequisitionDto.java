package projectlx.user.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import projectlx.user.management.model.DemoRequisitionStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class DemoRequisitionDto {
    private Long id;
    private String requisitionNumber;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String demoRequest;
    private DemoRequisitionStatus status;
    private String assignedHandlerUsername;
    private String adminNotes;
    private LocalDateTime contactedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
