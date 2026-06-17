package projectlx.messaging.inbound.business.auditable.api;

import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.model.BotSession;

import java.util.Locale;

public interface BotSessionServiceAuditable {

    BotSession createSession(BotSession session, Locale locale, String username);

    BotSession updateSession(BotSession session, Locale locale, String username);

    BotMessage createMessage(BotMessage message, Locale locale, String username);
}
