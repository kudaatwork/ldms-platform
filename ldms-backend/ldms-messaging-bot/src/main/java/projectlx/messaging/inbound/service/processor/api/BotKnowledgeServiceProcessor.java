package projectlx.messaging.inbound.service.processor.api;

import projectlx.messaging.inbound.utils.responses.BotKnowledgeResponse;

import java.util.Locale;

public interface BotKnowledgeServiceProcessor {

    BotKnowledgeResponse reload(Locale locale);

    BotKnowledgeResponse status(Locale locale);
}
