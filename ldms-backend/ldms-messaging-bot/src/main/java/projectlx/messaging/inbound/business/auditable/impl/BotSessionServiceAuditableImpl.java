package projectlx.messaging.inbound.business.auditable.impl;

import projectlx.messaging.inbound.business.auditable.api.BotSessionServiceAuditable;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.model.BotSession;
import projectlx.messaging.inbound.repository.BotMessageRepository;
import projectlx.messaging.inbound.repository.BotSessionRepository;

import java.util.Locale;

public class BotSessionServiceAuditableImpl implements BotSessionServiceAuditable {

    private final BotSessionRepository botSessionRepository;
    private final BotMessageRepository botMessageRepository;

    public BotSessionServiceAuditableImpl(BotSessionRepository botSessionRepository,
                                          BotMessageRepository botMessageRepository) {
        this.botSessionRepository = botSessionRepository;
        this.botMessageRepository = botMessageRepository;
    }

    @Override
    public BotSession createSession(BotSession session, Locale locale, String username) {
        return botSessionRepository.save(session);
    }

    @Override
    public BotSession updateSession(BotSession session, Locale locale, String username) {
        return botSessionRepository.save(session);
    }

    @Override
    public BotMessage createMessage(BotMessage message, Locale locale, String username) {
        return botMessageRepository.save(message);
    }
}
