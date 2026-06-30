package projectlx.messaging.inbound.business.logic.api;

import projectlx.messaging.inbound.utils.requests.RateBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.SendBotMessageRequest;
import projectlx.messaging.inbound.utils.requests.StartBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.UpdateBotAssistantModeRequest;
import projectlx.messaging.inbound.utils.responses.BotSessionResponse;

import java.util.Locale;

public interface BotSessionService {

    BotSessionResponse startSession(StartBotSessionRequest request, Locale locale, String username);

    BotSessionResponse sendMessage(SendBotMessageRequest request, Locale locale, String username);

    BotSessionResponse listMySessions(Locale locale, String username);

    BotSessionResponse findMySessionById(String sessionId, Locale locale, String username);

    BotSessionResponse listAllSessions(Locale locale);

    BotSessionResponse findSessionById(String sessionId, Locale locale);

    BotSessionResponse rateSession(RateBotSessionRequest request, Locale locale, String username);

    BotSessionResponse updateAssistantMode(UpdateBotAssistantModeRequest request, Locale locale, String username);

    BotSessionResponse getPricing(Locale locale);

    BotSessionResponse startGuestSession(StartBotSessionRequest request, Locale locale);

    BotSessionResponse sendGuestMessage(SendBotMessageRequest request, Locale locale);

    BotSessionResponse findGuestSessionById(String sessionId, Locale locale);
}
