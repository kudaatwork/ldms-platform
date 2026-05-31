package projectlx.user.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.model.SupportTicketCategory;
import projectlx.user.management.model.SupportTicketPriority;
import projectlx.user.management.model.SupportTicketStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class SupportTicketDto {
    private Long id;
    private String ticketNumber;
    private String subject;
    private String description;
    private SupportTicketCategory category;
    private SupportTicketPriority priority;
    private SupportTicketStatus status;
    private String requesterUsername;
    private String requesterEmail;
    private Long organizationId;
    private String organizationName;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
