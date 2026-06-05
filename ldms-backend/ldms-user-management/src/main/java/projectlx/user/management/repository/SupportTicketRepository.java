package projectlx.user.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.SupportTicket;
import projectlx.user.management.model.SupportTicketStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByRequesterUsernameAndEntityStatusNotOrderByCreatedAtDesc(
            String requesterUsername, EntityStatus entityStatus);

    Optional<SupportTicket> findByIdAndRequesterUsernameAndEntityStatusNot(
            Long id, String requesterUsername, EntityStatus entityStatus);

    Optional<SupportTicket> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    List<SupportTicket> findByEntityStatusNotOrderByCreatedAtDesc(EntityStatus entityStatus);

    long countByAssignedHandlerUserIdAndStatusInAndEntityStatusNot(
            Long assignedHandlerUserId,
            Collection<SupportTicketStatus> statuses,
            EntityStatus entityStatus);
}
