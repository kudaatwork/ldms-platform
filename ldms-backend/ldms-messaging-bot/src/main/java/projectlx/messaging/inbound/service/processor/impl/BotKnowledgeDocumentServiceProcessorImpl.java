package projectlx.messaging.inbound.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeDocumentService;
import projectlx.messaging.inbound.service.processor.api.BotKnowledgeDocumentServiceProcessor;
import projectlx.messaging.inbound.utils.responses.BotKnowledgeDocumentResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class BotKnowledgeDocumentServiceProcessorImpl implements BotKnowledgeDocumentServiceProcessor {

    private final BotKnowledgeDocumentService botKnowledgeDocumentService;

    @Override
    public BotKnowledgeDocumentResponse listAll(Locale locale) {
        return botKnowledgeDocumentService.listAll(locale);
    }

    @Override
    public BotKnowledgeDocumentResponse upload(String title, MultipartFile file,
                                               Locale locale, String username) {
        return botKnowledgeDocumentService.upload(title, file, locale, username);
    }

    @Override
    public BotKnowledgeDocumentResponse createFromText(String title, String body,
                                                       Locale locale, String username) {
        return botKnowledgeDocumentService.createFromText(title, body, locale, username);
    }

    @Override
    public BotKnowledgeDocumentResponse delete(Long id, Locale locale, String username) {
        return botKnowledgeDocumentService.delete(id, locale, username);
    }
}
