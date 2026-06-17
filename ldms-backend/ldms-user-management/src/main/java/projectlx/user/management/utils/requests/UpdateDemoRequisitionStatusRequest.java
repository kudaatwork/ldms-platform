package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import projectlx.user.management.model.DemoRequisitionStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateDemoRequisitionStatusRequest {

    @NotNull
    private Long demoRequisitionId;

    @NotNull
    private DemoRequisitionStatus status;

    @Size(max = 4000)
    private String adminNotes;

    private LocalDateTime scheduledAt;
}
