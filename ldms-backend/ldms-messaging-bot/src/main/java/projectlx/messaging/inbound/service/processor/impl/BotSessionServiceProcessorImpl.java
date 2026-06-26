package projectlx.messaging.inbound.service.processor.impl;

import projectlx.messaging.inbound.business.logic.api.BotSessionService;
import projectlx.messaging.inbound.service.processor.api.BotSessionServiceProcessor;
import projectlx.messaging.inbound.utils.requests.RateBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.SendBotMessageRequest;
import projectlx.messaging.inbound.utils.requests.StartBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.UpdateBotAssistantModeRequest;
import projectlx.messaging.inbound.utils.responses.BotSessionResponse;

import java.util.Locale;

public class BotSessionServiceProcessorImpl implements BotSessionServiceProcessor {

    private final BotSessionService botSessionService;

    public BotSessionServiceProcessorImpl(BotSessionService botSessionService) {
        this.botSessionService = botSessionService;
    }

    @Override
    public BotSessionResponse startSession(StartBotSessionRequest request, Locale locale, String username) {
        return botSessionService.startSession(request, locale, username);
    }

    @Override
    public BotSessionResponse sendMessage(SendBotMessageRequest request, Locale locale, String username) {
        return botSessionService.sendMessage(request, locale, username);
    }

    @Override
    public BotSessionResponse listMySessions(Locale locale, String username) {
        return botSessionService.listMySessions(locale, username);
    }

    @Override
    public BotSessionResponse findMySessionById(String sessionId, Locale locale, String username) {
        return botSessionService.findMySessionById(sessionId, locale, username);
    }

    @Override
    public BotSessionResponse listAllSessions(Locale locale) {
        return botSessionService.listAllSessions(locale);
    }

    @Override
    public BotSessionResponse findSessionById(String sessionId, Locale locale) {
        return botSessionService.findSessionById(sessionId, locale);
    }

    @Override
    public BotSessionResponse rateSession(RateBotSessionRequest request, Locale locale, String username) {
        return botSessionService.rateSession(request, locale, username);
    }

    @Override
    public BotSessionResponse updateAssistantMode(UpdateBotAssistantModeRequest request, Locale locale, String username) {
        return botSessionService.updateAssistantMode(request, locale, username);
    }

    @Override
    public BotSessionResponse getPricing(Locale locale) {
        return botSessionService.getPricing(locale);
    }
}
