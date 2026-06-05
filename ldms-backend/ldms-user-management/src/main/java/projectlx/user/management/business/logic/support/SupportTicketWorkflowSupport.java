package projectlx.user.management.business.logic.support;

import projectlx.user.management.model.SupportTicketStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class SupportTicketWorkflowSupport {

    private static final Map<SupportTicketStatus, Set<SupportTicketStatus>> ALLOWED = Map.of(
            SupportTicketStatus.OPEN, EnumSet.of(SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.CLOSED),
            SupportTicketStatus.IN_PROGRESS,
                    EnumSet.of(SupportTicketStatus.WAITING_ON_CUSTOMER, SupportTicketStatus.RESOLVED, SupportTicketStatus.CLOSED),
            SupportTicketStatus.WAITING_ON_CUSTOMER,
                    EnumSet.of(SupportTicketStatus.IN_PROGRESS, SupportTicketStatus.RESOLVED),
            SupportTicketStatus.RESOLVED, EnumSet.of(SupportTicketStatus.CLOSED, SupportTicketStatus.OPEN),
            SupportTicketStatus.CLOSED, EnumSet.noneOf(SupportTicketStatus.class));

    private SupportTicketWorkflowSupport() {
    }

    public static boolean canTransition(SupportTicketStatus from, SupportTicketStatus to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(SupportTicketStatus.class)).contains(to);
    }
}
