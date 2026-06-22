package projectlx.messaging.inbound.business.auditable.api;

import projectlx.messaging.inbound.model.BotKnowledgeDocument;

import java.util.Locale;

public interface BotKnowledgeDocumentServiceAuditable {

    BotKnowledgeDocument create(BotKnowledgeDocument document, Locale locale, String username);

    BotKnowledgeDocument delete(BotKnowledgeDocument document, Locale locale, String username);
}
