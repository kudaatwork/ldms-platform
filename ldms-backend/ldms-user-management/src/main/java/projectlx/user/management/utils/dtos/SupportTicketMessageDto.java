package projectlx.user.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.model.SupportTicketMessageAuthorRole;
import projectlx.user.management.model.SupportTicketMessageVisibility;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class SupportTicketMessageDto {
    private Long id;
    private Long supportTicketId;
    private String authorUsername;
    private SupportTicketMessageAuthorRole authorRole;
    private SupportTicketMessageVisibility visibility;
    private String body;
    private LocalDateTime createdAt;
}
