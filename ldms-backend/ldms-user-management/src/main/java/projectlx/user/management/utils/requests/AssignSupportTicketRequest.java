package projectlx.user.management.utils.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AssignSupportTicketRequest {

    @NotNull
    private Long supportTicketId;

    /** When null, assigns to the acting handler username. */
    private Long handlerUserId;
}
