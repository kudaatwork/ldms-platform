package projectlx.messaging.inbound.business.validator.impl;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.messaging.inbound.business.validator.api.BotSessionServiceValidator;
import projectlx.messaging.inbound.utils.enums.I18Code;
import projectlx.messaging.inbound.utils.requests.RateBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.SendBotMessageRequest;
import projectlx.messaging.inbound.utils.requests.StartBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.UpdateBotAssistantModeRequest;
import projectlx.messaging.inbound.utils.enums.BotAssistantMode;

import java.util.Locale;

public class BotSessionServiceValidatorImpl implements BotSessionServiceValidator {

    private final MessageService messageService;

    public BotSessionServiceValidatorImpl(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public ValidatorDto isStartSessionRequestValid(StartBotSessionRequest request, Locale locale) {
        if (request == null) {
            return invalid(I18Code.MESSAGE_BOT_START_SESSION_INVALID, locale);
        }
        return valid();
    }

    @Override
    public ValidatorDto isSendMessageRequestValid(SendBotMessageRequest request, Locale locale) {
        if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()
                || request.getBody() == null || request.getBody().isBlank()) {
            return invalid(I18Code.MESSAGE_BOT_SEND_MESSAGE_INVALID, locale);
        }
        if (request.getAssistantMode() != null && !request.getAssistantMode().isBlank()) {
            String mode = request.getAssistantMode().trim().toUpperCase(Locale.ROOT);
            if (!"ASSISTANT".equals(mode) && !"AGENT".equals(mode)) {
                return invalid(I18Code.MESSAGE_BOT_ASSISTANT_MODE_INVALID, locale);
            }
        }
        return valid();
    }

    @Override
    public ValidatorDto isRateSessionRequestValid(RateBotSessionRequest request, Locale locale) {
        if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()
                || request.getScore() == null || request.getScore() < 1 || request.getScore() > 5) {
            return invalid(I18Code.MESSAGE_BOT_RATE_SESSION_INVALID, locale);
        }
        return valid();
    }

    @Override
    public ValidatorDto isUpdateAssistantModeRequestValid(UpdateBotAssistantModeRequest request, Locale locale) {
        if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()
                || request.getAssistantMode() == null || request.getAssistantMode().isBlank()) {
            return invalid(I18Code.MESSAGE_BOT_ASSISTANT_MODE_INVALID, locale);
        }
        String mode = request.getAssistantMode().trim().toUpperCase(Locale.ROOT);
        if (!"ASSISTANT".equals(mode) && !"AGENT".equals(mode)) {
            return invalid(I18Code.MESSAGE_BOT_ASSISTANT_MODE_INVALID, locale);
        }
        return valid();
    }

    @Override
    public ValidatorDto isSessionIdValid(String sessionId, Locale locale) {
        if (sessionId == null || sessionId.isBlank()) {
            return invalid(I18Code.MESSAGE_BOT_SESSION_NOT_FOUND, locale);
        }
        return valid();
    }

    private ValidatorDto valid() {
        ValidatorDto dto = new ValidatorDto();
        dto.setSuccess(true);
        return dto;
    }

    private ValidatorDto invalid(I18Code code, Locale locale) {
        ValidatorDto dto = new ValidatorDto();
        dto.setSuccess(false);
        dto.setErrorMessages(java.util.List.of(messageService.getMessage(code.getCode(), new String[]{}, locale)));
        return dto;
    }
}
