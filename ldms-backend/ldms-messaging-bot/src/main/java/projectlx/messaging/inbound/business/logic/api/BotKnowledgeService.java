package projectlx.messaging.inbound.business.logic.api;

import projectlx.messaging.inbound.utils.dtos.BotKnowledgeStatusDto;

public interface BotKnowledgeService {

    BotKnowledgeStatusDto reload();

    BotKnowledgeStatusDto status();
}
