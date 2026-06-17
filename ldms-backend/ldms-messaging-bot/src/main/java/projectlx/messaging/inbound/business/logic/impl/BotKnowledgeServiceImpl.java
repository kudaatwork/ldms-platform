package projectlx.messaging.inbound.business.logic.impl;

import lombok.RequiredArgsConstructor;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeService;
import projectlx.messaging.inbound.business.logic.support.LdmsKnowledgeContextSupport;
import projectlx.messaging.inbound.utils.dtos.BotKnowledgeStatusDto;

@RequiredArgsConstructor
public class BotKnowledgeServiceImpl implements BotKnowledgeService {

    private final LdmsKnowledgeContextSupport knowledgeContextSupport;

    @Override
    public BotKnowledgeStatusDto reload() {
        knowledgeContextSupport.reload();
        return knowledgeContextSupport.status();
    }

    @Override
    public BotKnowledgeStatusDto status() {
        return knowledgeContextSupport.status();
    }
}
