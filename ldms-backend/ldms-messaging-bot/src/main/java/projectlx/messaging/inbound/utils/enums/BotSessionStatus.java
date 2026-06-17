package projectlx.messaging.inbound.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BotSessionStatus {
    ACTIVE("Active"),
    WAITING("Awaiting reply"),
    RESOLVED("Resolved"),
    ESCALATED("Escalated to agent");

    private final String label;
}
