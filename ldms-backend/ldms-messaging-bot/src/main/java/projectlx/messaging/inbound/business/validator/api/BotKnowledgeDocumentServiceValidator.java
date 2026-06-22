package projectlx.messaging.inbound.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface BotKnowledgeDocumentServiceValidator {

    ValidatorDto isUploadRequestValid(String title, String originalFilename, Locale locale);

    ValidatorDto isCreateTextRequestValid(String title, String body, Locale locale);

    ValidatorDto isIdValid(Long id, Locale locale);
}
