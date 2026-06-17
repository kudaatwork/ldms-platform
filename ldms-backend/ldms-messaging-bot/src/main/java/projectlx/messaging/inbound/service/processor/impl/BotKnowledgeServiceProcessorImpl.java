package projectlx.messaging.inbound.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeService;
import projectlx.messaging.inbound.service.processor.api.BotKnowledgeServiceProcessor;
import projectlx.messaging.inbound.utils.responses.BotKnowledgeResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class BotKnowledgeServiceProcessorImpl implements BotKnowledgeServiceProcessor {

    private final BotKnowledgeService botKnowledgeService;

    @Override
    public BotKnowledgeResponse reload(Locale locale) {
        BotKnowledgeResponse response = new BotKnowledgeResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Bot knowledge reloaded");
        response.setKnowledgeStatus(botKnowledgeService.reload());
        return response;
    }

    @Override
    public BotKnowledgeResponse status(Locale locale) {
        BotKnowledgeResponse response = new BotKnowledgeResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage("Bot knowledge status");
        response.setKnowledgeStatus(botKnowledgeService.status());
        return response;
    }
}
