package projectlx.messaging.inbound.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.messaging.inbound.utils.requests.RateBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.SendBotMessageRequest;
import projectlx.messaging.inbound.utils.requests.StartBotSessionRequest;

import java.util.Locale;

public interface BotSessionServiceValidator {

    ValidatorDto isStartSessionRequestValid(StartBotSessionRequest request, Locale locale);

    ValidatorDto isSendMessageRequestValid(SendBotMessageRequest request, Locale locale);

    ValidatorDto isRateSessionRequestValid(RateBotSessionRequest request, Locale locale);

    ValidatorDto isSessionIdValid(String sessionId, Locale locale);
}
