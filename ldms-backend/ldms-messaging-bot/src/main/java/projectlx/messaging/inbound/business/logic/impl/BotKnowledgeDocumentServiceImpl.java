package projectlx.messaging.inbound.business.logic.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.messaging.inbound.business.auditable.api.BotKnowledgeDocumentServiceAuditable;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeDocumentService;
import projectlx.messaging.inbound.business.logic.support.BotKnowledgeDocumentRagSupport;
import projectlx.messaging.inbound.business.logic.support.BotPdfTextExtractorSupport;
import projectlx.messaging.inbound.business.validator.api.BotKnowledgeDocumentServiceValidator;
import projectlx.messaging.inbound.model.BotKnowledgeDocument;
import projectlx.messaging.inbound.repository.BotKnowledgeDocumentRepository;
import projectlx.messaging.inbound.utils.dtos.BotKnowledgeDocumentDto;
import projectlx.messaging.inbound.utils.enums.I18Code;
import projectlx.messaging.inbound.utils.responses.BotKnowledgeDocumentResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Transactional
public class BotKnowledgeDocumentServiceImpl implements BotKnowledgeDocumentService {

    private final BotKnowledgeDocumentServiceValidator validator;
    private final BotKnowledgeDocumentServiceAuditable auditable;
    private final BotKnowledgeDocumentRepository botKnowledgeDocumentRepository;
    private final BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport;
    private final BotPdfTextExtractorSupport botPdfTextExtractorSupport;
    private final MessageService messageService;

    public BotKnowledgeDocumentServiceImpl(BotKnowledgeDocumentServiceValidator validator,
                                           BotKnowledgeDocumentServiceAuditable auditable,
                                           BotKnowledgeDocumentRepository botKnowledgeDocumentRepository,
                                           BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport,
                                           BotPdfTextExtractorSupport botPdfTextExtractorSupport,
                                           MessageService messageService) {
        this.validator = validator;
        this.auditable = auditable;
        this.botKnowledgeDocumentRepository = botKnowledgeDocumentRepository;
        this.botKnowledgeDocumentRagSupport = botKnowledgeDocumentRagSupport;
        this.botPdfTextExtractorSupport = botPdfTextExtractorSupport;
        this.messageService = messageService;
    }

    @Override
    @Transactional(readOnly = true)
    public BotKnowledgeDocumentResponse listAll(Locale locale) {
        List<BotKnowledgeDocumentDto> rows = botKnowledgeDocumentRepository
                .findByEntityStatusNotOrderByModifiedAtDescCreatedAtDesc(EntityStatus.DELETED)
                .stream()
                .map(BotKnowledgeDocumentServiceImpl::toDto)
                .toList();
        BotKnowledgeDocumentResponse response = success(I18Code.MESSAGE_BOT_DOCUMENT_LIST_SUCCESS, locale);
        response.setBotKnowledgeDocumentDtoList(rows);
        return response;
    }

    @Override
    public BotKnowledgeDocumentResponse upload(String title, MultipartFile file,
                                               Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        String originalFilename = file != null ? file.getOriginalFilename() : null;
        var validation = validator.isUploadRequestValid(title, originalFilename, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }

        // ============================================================
        // STEP 2: Extract text from uploaded file
        // ============================================================
        String extractedText = "";
        try {
            extractedText = botPdfTextExtractorSupport.extractText(file);
        } catch (Exception ex) {
            log.warn("Text extraction failed for '{}': {}", originalFilename, ex.getMessage());
        }

        // ============================================================
        // STEP 3: Persist knowledge document
        // ============================================================
        LocalDateTime now = LocalDateTime.now();
        BotKnowledgeDocument document = new BotKnowledgeDocument();
        document.setTitle(title.trim());
        document.setOriginalFilename(originalFilename);
        document.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        document.setExtractedText(extractedText.isBlank() ? null : extractedText);
        document.setFileSizeBytes(file.getSize());
        document.setPublished(true);
        document.setUseCount(0);
        document.setEntityStatus(EntityStatus.ACTIVE);
        document.setCreatedAt(now);
        document.setCreatedBy(username);
        document.setModifiedAt(now);
        document.setModifiedBy(username);

        BotKnowledgeDocument saved = auditable.create(document, locale, username);

        // ============================================================
        // STEP 4: Reload document RAG cache
        // ============================================================
        botKnowledgeDocumentRagSupport.reload();

        BotKnowledgeDocumentResponse response = success(I18Code.MESSAGE_BOT_DOCUMENT_UPLOADED, locale);
        response.setBotKnowledgeDocumentDto(toDto(saved));
        return response;
    }

    @Override
    public BotKnowledgeDocumentResponse createFromText(String title, String body,
                                                       Locale locale, String username) {
        var validation = validator.isCreateTextRequestValid(title, body, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }

        String trimmedBody = body.trim();
        LocalDateTime now = LocalDateTime.now();
        BotKnowledgeDocument document = new BotKnowledgeDocument();
        document.setTitle(title.trim());
        document.setOriginalFilename("pasted-text.txt");
        document.setContentType("text/plain");
        document.setExtractedText(trimmedBody);
        document.setFileSizeBytes(trimmedBody.length());
        document.setPublished(true);
        document.setUseCount(0);
        document.setEntityStatus(EntityStatus.ACTIVE);
        document.setCreatedAt(now);
        document.setCreatedBy(username);
        document.setModifiedAt(now);
        document.setModifiedBy(username);

        BotKnowledgeDocument saved = auditable.create(document, locale, username);
        botKnowledgeDocumentRagSupport.reload();

        BotKnowledgeDocumentResponse response = success(I18Code.MESSAGE_BOT_DOCUMENT_TEXT_CREATED, locale);
        response.setBotKnowledgeDocumentDto(toDto(saved));
        return response;
    }

    @Override
    public BotKnowledgeDocumentResponse delete(Long id, Locale locale, String username) {
        var validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }
        BotKnowledgeDocument document = botKnowledgeDocumentRepository
                .findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElse(null);
        if (document == null) {
            return failure(404, messageService.getMessage(
                    I18Code.MESSAGE_BOT_DOCUMENT_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        document.setEntityStatus(EntityStatus.DELETED);
        document.setModifiedAt(LocalDateTime.now());
        document.setModifiedBy(username);
        auditable.delete(document, locale, username);

        botKnowledgeDocumentRagSupport.reload();
        return success(I18Code.MESSAGE_BOT_DOCUMENT_DELETED, locale);
    }

    static BotKnowledgeDocumentDto toDto(BotKnowledgeDocument doc) {
        BotKnowledgeDocumentDto dto = new BotKnowledgeDocumentDto();
        dto.setId(doc.getId());
        dto.setTitle(doc.getTitle());
        dto.setOriginalFilename(doc.getOriginalFilename());
        dto.setContentType(doc.getContentType());
        dto.setFileSizeBytes(doc.getFileSizeBytes());
        dto.setPublished(doc.isPublished());
        dto.setUseCount(doc.getUseCount());
        dto.setExtractedTextLength(doc.getExtractedText() != null ? doc.getExtractedText().length() : 0);
        dto.setCreatedAt(doc.getCreatedAt());
        dto.setModifiedAt(doc.getModifiedAt());
        return dto;
    }

    private BotKnowledgeDocumentResponse success(I18Code code, Locale locale) {
        BotKnowledgeDocumentResponse response = new BotKnowledgeDocumentResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(code.getCode(), new String[]{}, locale));
        return response;
    }

    private BotKnowledgeDocumentResponse failure(int statusCode, String message) {
        BotKnowledgeDocumentResponse response = new BotKnowledgeDocumentResponse();
        response.setSuccess(false);
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }
}
