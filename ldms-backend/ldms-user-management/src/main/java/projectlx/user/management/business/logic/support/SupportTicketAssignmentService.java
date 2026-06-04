package projectlx.user.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.SupportTicketStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.repository.SupportTicketRepository;
import projectlx.user.management.repository.UserRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Assigns new support tickets to the least-loaded operational issue handler.
 * Handler candidates are admin-portal users ({@code operationalIssueHandler=true}, no organisation).
 */
@Slf4j
@RequiredArgsConstructor
public class SupportTicketAssignmentService {

    private static final Set<SupportTicketStatus> OPEN_WORKLOAD_STATUSES = Set.of(
            SupportTicketStatus.OPEN,
            SupportTicketStatus.IN_PROGRESS,
            SupportTicketStatus.WAITING_ON_CUSTOMER);

    private final UserRepository userRepository;
    private final SupportTicketRepository supportTicketRepository;

    public Optional<User> pickHandler() {
        List<User> candidates = userRepository
                .findByOperationalIssueHandlerTrueAndOrganizationIdIsNullAndEntityStatusNot(EntityStatus.DELETED);
        if (candidates.isEmpty()) {
            log.warn("No operational issue handler candidates available; support ticket left unassigned");
            return Optional.empty();
        }

        Map<Long, Long> loads = new HashMap<>();
        for (User candidate : candidates) {
            loads.put(candidate.getId(), countOpenTickets(candidate.getId()));
        }

        return candidates.stream()
                .min(Comparator
                        .comparingLong((User user) -> loads.getOrDefault(user.getId(), 0L))
                        .thenComparing(User::getId));
    }

    private long countOpenTickets(Long handlerUserId) {
        return supportTicketRepository
                .countByAssignedHandlerUserIdAndStatusInAndEntityStatusNot(
                        handlerUserId, OPEN_WORKLOAD_STATUSES, EntityStatus.DELETED);
    }
}
