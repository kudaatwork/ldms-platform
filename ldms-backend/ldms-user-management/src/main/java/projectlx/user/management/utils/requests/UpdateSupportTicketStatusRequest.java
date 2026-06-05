package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.model.SupportTicketStatus;

@Getter
@Setter
@ToString
public class UpdateSupportTicketStatusRequest {

    @NotNull
    private Long supportTicketId;

    @NotNull
    private SupportTicketStatus status;
}
