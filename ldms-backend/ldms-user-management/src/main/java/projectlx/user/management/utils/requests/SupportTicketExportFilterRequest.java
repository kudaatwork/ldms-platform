package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.model.SupportTicketStatus;

@Getter
@Setter
@ToString
public class SupportTicketExportFilterRequest {
    private SupportTicketStatus status;
    private String search;
}
