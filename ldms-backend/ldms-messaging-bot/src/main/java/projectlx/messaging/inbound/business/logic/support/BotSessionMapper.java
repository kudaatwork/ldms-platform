package projectlx.messaging.inbound.business.logic.support;

import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.model.BotSession;
import projectlx.messaging.inbound.repository.BotMessageRepository;
import projectlx.messaging.inbound.utils.dtos.BotMessageDto;
import projectlx.messaging.inbound.utils.dtos.BotSessionDto;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BotSessionMapper {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BotMessageRepository botMessageRepository;

    public BotSessionMapper(BotMessageRepository botMessageRepository) {
        this.botMessageRepository = botMessageRepository;
    }

    public static String newSessionPublicId() {
        return "bot-" + UUID.randomUUID().toString().substring(0, 4);
    }

    public BotSessionDto toDto(BotSession session, boolean includeMessages) {
        BotSessionDto dto = new BotSessionDto();
        dto.setSessionId(session.getSessionId());
        dto.setUserDisplayName(session.getUserDisplayName());
        dto.setUserPhone(session.getUserPhone() != null ? session.getUserPhone() : "");
        dto.setOrganizationName(session.getOrganizationName() != null ? session.getOrganizationName() : "—");
        dto.setChannel(session.getChannel().name());
        dto.setStatus(session.getStatus().name());
        dto.setStatusLabel(session.getStatus().getLabel());
        dto.setTopic(session.getTopic() != null ? session.getTopic() : "General LDMS question");
        dto.setSatisfactionScore(session.getSatisfactionScore());

        List<BotMessage> messages = botMessageRepository.findByBotSessionIdAndEntityStatusOrderByCreatedAtAsc(
                session.getId(), EntityStatus.ACTIVE);
        dto.setMessageCount((int) messages.size());
        if (!messages.isEmpty()) {
            BotMessage last = messages.get(messages.size() - 1);
            dto.setLastMessageAt(ISO.format(last.getCreatedAt()));
        } else {
            dto.setLastMessageAt(ISO.format(session.getCreatedAt()));
        }

        if (includeMessages) {
            dto.setMessages(messages.stream().map(this::toMessageDto).toList());
        }
        return dto;
    }

    public List<BotSessionDto> toDtoList(List<BotSession> sessions, boolean includeMessages) {
        return sessions.stream()
                .sorted(Comparator.comparing(
                        (BotSession s) -> s.getModifiedAt() != null ? s.getModifiedAt() : s.getCreatedAt()).reversed())
                .map(s -> toDto(s, includeMessages))
                .toList();
    }

    private BotMessageDto toMessageDto(BotMessage message) {
        BotMessageDto dto = new BotMessageDto();
        dto.setId("m" + message.getId());
        dto.setRole(message.getRole().getWireValue());
        dto.setBody(message.getBody());
        dto.setSentAt(ISO.format(message.getCreatedAt()));
        return dto;
    }
}
