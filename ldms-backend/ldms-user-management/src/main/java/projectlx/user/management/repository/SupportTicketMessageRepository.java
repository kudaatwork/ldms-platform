package projectlx.user.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.SupportTicketMessage;
import projectlx.user.management.model.SupportTicketMessageVisibility;

import java.util.List;

public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, Long> {

    List<SupportTicketMessage> findBySupportTicketIdAndEntityStatusNotOrderByCreatedAtAsc(
            Long supportTicketId, EntityStatus entityStatus);

    List<SupportTicketMessage> findBySupportTicketIdAndEntityStatusNotAndVisibilityOrderByCreatedAtAsc(
            Long supportTicketId, EntityStatus entityStatus, SupportTicketMessageVisibility visibility);
}
