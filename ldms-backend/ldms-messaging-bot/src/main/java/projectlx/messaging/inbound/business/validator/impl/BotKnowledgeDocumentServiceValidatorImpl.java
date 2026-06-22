package projectlx.messaging.inbound.business.validator.impl;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.messaging.inbound.business.validator.api.BotKnowledgeDocumentServiceValidator;
import projectlx.messaging.inbound.utils.enums.I18Code;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BotKnowledgeDocumentServiceValidatorImpl implements BotKnowledgeDocumentServiceValidator {

    private final MessageService messageService;

    public BotKnowledgeDocumentServiceValidatorImpl(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public ValidatorDto isUploadRequestValid(String title, String originalFilename, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (title == null || title.isBlank()) {
            errors.add(msg(I18Code.MESSAGE_BOT_DOCUMENT_TITLE_REQUIRED, locale));
        }
        if (originalFilename == null || originalFilename.isBlank()) {
            errors.add(msg(I18Code.MESSAGE_BOT_DOCUMENT_FILE_REQUIRED, locale));
        }
        return toDto(errors);
    }

    @Override
    public ValidatorDto isCreateTextRequestValid(String title, String body, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (title == null || title.isBlank()) {
            errors.add(msg(I18Code.MESSAGE_BOT_DOCUMENT_TITLE_REQUIRED, locale));
        }
        if (body == null || body.isBlank()) {
            errors.add(msg(I18Code.MESSAGE_BOT_DOCUMENT_BODY_REQUIRED, locale));
        } else if (body.trim().length() < 20) {
            errors.add(msg(I18Code.MESSAGE_BOT_DOCUMENT_BODY_TOO_SHORT, locale));
        }
        return toDto(errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (id == null || id < 1) {
            errors.add(msg(I18Code.MESSAGE_BOT_DOCUMENT_NOT_FOUND, locale));
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
