package projectlx.messaging.inbound.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.messaging.inbound.service.processor.api.BotKnowledgeDocumentServiceProcessor;
import projectlx.messaging.inbound.utils.requests.CreateBotKnowledgeTextRequest;
import projectlx.messaging.inbound.utils.responses.BotKnowledgeDocumentResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-messaging-inbound/v1/backoffice/bot-knowledge-document")
@Tag(name = "Bot Knowledge Documents (backoffice)", description = "Upload and manage PDF knowledge documents for bot RAG")
@RequiredArgsConstructor
public class BotKnowledgeDocumentBackofficeResource {

    private final BotKnowledgeDocumentServiceProcessor botKnowledgeDocumentServiceProcessor;

    @Auditable(action = "BACKOFFICE_LIST_BOT_KNOWLEDGE_DOCUMENTS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    @Operation(summary = "List all uploaded knowledge documents")
    public ResponseEntity<BotKnowledgeDocumentResponse> list(
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        BotKnowledgeDocumentResponse response = botKnowledgeDocumentServiceProcessor.listAll(locale);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_UPLOAD_BOT_KNOWLEDGE_DOCUMENT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF knowledge document for bot RAG",
               description = "Accepts PDF or plain-text files. Text is extracted and indexed for keyword retrieval.")
    public ResponseEntity<BotKnowledgeDocumentResponse> upload(
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotKnowledgeDocumentResponse response =
                botKnowledgeDocumentServiceProcessor.upload(title, file, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_CREATE_BOT_KNOWLEDGE_TEXT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create-text")
    @Operation(summary = "Save pasted text as bot knowledge",
               description = "Stores free-form text content for keyword RAG retrieval.")
    public ResponseEntity<BotKnowledgeDocumentResponse> createText(
            @RequestBody CreateBotKnowledgeTextRequest request,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotKnowledgeDocumentResponse response = botKnowledgeDocumentServiceProcessor.createFromText(
                request.getTitle(), request.getBody(), locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @Auditable(action = "BACKOFFICE_DELETE_BOT_KNOWLEDGE_DOCUMENT")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Soft-delete a knowledge document")
    public ResponseEntity<BotKnowledgeDocumentResponse> delete(
            @PathVariable Long id,
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        BotKnowledgeDocumentResponse response =
                botKnowledgeDocumentServiceProcessor.delete(id, locale, username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
