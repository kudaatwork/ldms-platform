package projectlx.messaging.inbound.business.logic.impl;

import lombok.RequiredArgsConstructor;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeService;
import projectlx.messaging.inbound.business.logic.support.BotFaqRagSupport;
import projectlx.messaging.inbound.business.logic.support.BotKnowledgeDocumentRagSupport;
import projectlx.messaging.inbound.business.logic.support.LdmsKnowledgeContextSupport;
import projectlx.messaging.inbound.utils.dtos.BotKnowledgeStatusDto;

@RequiredArgsConstructor
public class BotKnowledgeServiceImpl implements BotKnowledgeService {

    private final LdmsKnowledgeContextSupport knowledgeContextSupport;
    private final BotFaqRagSupport botFaqRagSupport;
    private final BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport;

    @Override
    public BotKnowledgeStatusDto reload() {
        knowledgeContextSupport.reload();
        botFaqRagSupport.reload();
        botKnowledgeDocumentRagSupport.reload();
        return knowledgeContextSupport.status(botFaqRagSupport, botKnowledgeDocumentRagSupport);
    }

    @Override
    public BotKnowledgeStatusDto status() {
        return knowledgeContextSupport.status(botFaqRagSupport, botKnowledgeDocumentRagSupport);
    }
}
