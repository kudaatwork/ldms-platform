package projectlx.messaging.inbound.business.validator.impl;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.messaging.inbound.business.validator.api.BotFaqServiceValidator;
import projectlx.messaging.inbound.utils.enums.I18Code;
import projectlx.messaging.inbound.utils.requests.CreateBotFaqRequest;
import projectlx.messaging.inbound.utils.requests.EditBotFaqRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BotFaqServiceValidatorImpl implements BotFaqServiceValidator {

    private final MessageService messageService;

    public BotFaqServiceValidatorImpl(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public ValidatorDto isCreateRequestValid(CreateBotFaqRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add(msg(I18Code.MESSAGE_BOT_FAQ_INVALID_REQUEST, locale));
        } else {
            if (request.getQuestion() == null || request.getQuestion().isBlank()) {
                errors.add(msg(I18Code.MESSAGE_BOT_FAQ_QUESTION_REQUIRED, locale));
            }
            if (request.getAnswer() == null || request.getAnswer().isBlank()) {
                errors.add(msg(I18Code.MESSAGE_BOT_FAQ_ANSWER_REQUIRED, locale));
            }
        }
        return toDto(errors);
    }

    @Override
    public ValidatorDto isEditRequestValid(EditBotFaqRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add(msg(I18Code.MESSAGE_BOT_FAQ_INVALID_REQUEST, locale));
        } else {
            if (request.getQuestion() != null && request.getQuestion().isBlank()) {
                errors.add(msg(I18Code.MESSAGE_BOT_FAQ_QUESTION_REQUIRED, locale));
            }
            if (request.getAnswer() != null && request.getAnswer().isBlank()) {
                errors.add(msg(I18Code.MESSAGE_BOT_FAQ_ANSWER_REQUIRED, locale));
            }
        }
        return toDto(errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (id == null || id < 1) {
            errors.add(msg(I18Code.MESSAGE_BOT_FAQ_NOT_FOUND, locale));
        }
        return toDto(errors);
    }

    private String msg(I18Code code, Locale locale) {
        return messageService.getMessage(code.getCode(), new String[]{}, locale);
    }

    private static ValidatorDto toDto(List<String> errors) {
        ValidatorDto dto = new ValidatorDto();
        dto.setSuccess(errors.isEmpty());
        dto.setErrorMessages(errors);
        return dto;
    }
}
