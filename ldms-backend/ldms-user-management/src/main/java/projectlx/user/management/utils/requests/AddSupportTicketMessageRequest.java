package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.model.SupportTicketMessageVisibility;
import projectlx.user.management.model.SupportTicketStatus;

@Getter
@Setter
@ToString
public class AddSupportTicketMessageRequest {

    @NotNull
    private Long supportTicketId;

    @NotBlank
    @Size(min = 1, max = 8000)
    private String body;

    private SupportTicketMessageVisibility visibility;
}
