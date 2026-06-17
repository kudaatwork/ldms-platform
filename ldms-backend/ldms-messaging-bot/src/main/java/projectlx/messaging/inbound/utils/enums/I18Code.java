package projectlx.messaging.inbound.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum I18Code {
    MESSAGE_BOT_SESSION_CREATED("message.bot.session.created"),
    MESSAGE_BOT_SESSIONS_RETRIEVED("message.bot.sessions.retrieved"),
    MESSAGE_BOT_SESSION_RETRIEVED("message.bot.session.retrieved"),
    MESSAGE_BOT_SESSION_NOT_FOUND("message.bot.session.not.found"),
    MESSAGE_BOT_MESSAGE_SENT("message.bot.message.sent"),
    MESSAGE_BOT_SEND_MESSAGE_INVALID("message.bot.send.message.invalid.request"),
    MESSAGE_BOT_START_SESSION_INVALID("message.bot.start.session.invalid.request"),
    MESSAGE_BOT_ACCESS_DENIED("message.bot.access.denied"),
    MESSAGE_BOT_SESSION_RATED("message.bot.session.rated"),
    MESSAGE_BOT_RATE_SESSION_INVALID("message.bot.rate.session.invalid.request");

    private final String code;
}
