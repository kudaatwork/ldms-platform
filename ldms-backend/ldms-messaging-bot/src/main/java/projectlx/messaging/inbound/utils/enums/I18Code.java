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
    MESSAGE_BOT_RATE_SESSION_INVALID("message.bot.rate.session.invalid.request"),
    MESSAGE_BOT_FAQ_LIST_SUCCESS("message.bot.faq.list.success"),
    MESSAGE_BOT_FAQ_RETRIEVED("message.bot.faq.retrieved"),
    MESSAGE_BOT_FAQ_CREATED("message.bot.faq.created"),
    MESSAGE_BOT_FAQ_UPDATED("message.bot.faq.updated"),
    MESSAGE_BOT_FAQ_DELETED("message.bot.faq.deleted"),
    MESSAGE_BOT_FAQ_NOT_FOUND("message.bot.faq.not.found"),
    MESSAGE_BOT_FAQ_INVALID_REQUEST("message.bot.faq.invalid.request"),
    MESSAGE_BOT_FAQ_QUESTION_REQUIRED("message.bot.faq.question.required"),
    MESSAGE_BOT_FAQ_ANSWER_REQUIRED("message.bot.faq.answer.required"),
    MESSAGE_BOT_ANALYTICS_RETRIEVED("message.bot.analytics.retrieved"),
    MESSAGE_BOT_DOCUMENT_LIST_SUCCESS("message.bot.document.list.success"),
    MESSAGE_BOT_DOCUMENT_UPLOADED("message.bot.document.uploaded"),
    MESSAGE_BOT_DOCUMENT_DELETED("message.bot.document.deleted"),
    MESSAGE_BOT_DOCUMENT_NOT_FOUND("message.bot.document.not.found"),
    MESSAGE_BOT_DOCUMENT_TITLE_REQUIRED("message.bot.document.title.required"),
    MESSAGE_BOT_DOCUMENT_FILE_REQUIRED("message.bot.document.file.required"),
    MESSAGE_BOT_DOCUMENT_TEXT_CREATED("message.bot.document.text.created"),
    MESSAGE_BOT_DOCUMENT_BODY_REQUIRED("message.bot.document.body.required"),
    MESSAGE_BOT_DOCUMENT_BODY_TOO_SHORT("message.bot.document.body.too.short");

    private final String code;
}
