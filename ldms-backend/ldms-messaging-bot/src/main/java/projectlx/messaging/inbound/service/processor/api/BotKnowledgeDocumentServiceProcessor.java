package projectlx.messaging.inbound.service.processor.api;

import org.springframework.web.multipart.MultipartFile;
import projectlx.messaging.inbound.utils.responses.BotKnowledgeDocumentResponse;

import java.util.Locale;

public interface BotKnowledgeDocumentServiceProcessor {

    BotKnowledgeDocumentResponse listAll(Locale locale);

    BotKnowledgeDocumentResponse upload(String title, MultipartFile file, Locale locale, String username);

    BotKnowledgeDocumentResponse createFromText(String title, String body, Locale locale, String username);

    BotKnowledgeDocumentResponse delete(Long id, Locale locale, String username);
}
