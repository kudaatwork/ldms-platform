package projectlx.messaging.inbound.business.auditable.impl;

import projectlx.messaging.inbound.business.auditable.api.BotKnowledgeDocumentServiceAuditable;
import projectlx.messaging.inbound.model.BotKnowledgeDocument;
import projectlx.messaging.inbound.repository.BotKnowledgeDocumentRepository;

import java.util.Locale;

public class BotKnowledgeDocumentServiceAuditableImpl implements BotKnowledgeDocumentServiceAuditable {

    private final BotKnowledgeDocumentRepository botKnowledgeDocumentRepository;

    public BotKnowledgeDocumentServiceAuditableImpl(
            BotKnowledgeDocumentRepository botKnowledgeDocumentRepository) {
        this.botKnowledgeDocumentRepository = botKnowledgeDocumentRepository;
    }

    @Override
    public BotKnowledgeDocument create(BotKnowledgeDocument document, Locale locale, String username) {
        return botKnowledgeDocumentRepository.save(document);
    }

    @Override
    public BotKnowledgeDocument delete(BotKnowledgeDocument document, Locale locale, String username) {
        return botKnowledgeDocumentRepository.save(document);
    }
}
