package projectlx.messaging.inbound.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.messaging.inbound.business.auditable.api.BotFaqServiceAuditable;
import projectlx.messaging.inbound.business.logic.api.BotFaqService;
import projectlx.messaging.inbound.business.logic.support.BotFaqMapper;
import projectlx.messaging.inbound.business.logic.support.BotFaqRagSupport;
import projectlx.messaging.inbound.business.validator.api.BotFaqServiceValidator;
import projectlx.messaging.inbound.model.BotFaq;
import projectlx.messaging.inbound.repository.BotFaqRepository;
import projectlx.messaging.inbound.utils.dtos.BotFaqDto;
import projectlx.messaging.inbound.utils.enums.I18Code;
import projectlx.messaging.inbound.utils.requests.CreateBotFaqRequest;
import projectlx.messaging.inbound.utils.requests.EditBotFaqRequest;
import projectlx.messaging.inbound.utils.responses.BotFaqResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Transactional
public class BotFaqServiceImpl implements BotFaqService {

    private final BotFaqServiceValidator validator;
    private final BotFaqServiceAuditable auditable;
    private final BotFaqRepository botFaqRepository;
    private final BotFaqRagSupport botFaqRagSupport;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public BotFaqResponse listAll(Locale locale) {
        List<BotFaqDto> rows = botFaqRepository
                .findByEntityStatusNotOrderByModifiedAtDescCreatedAtDesc(EntityStatus.DELETED)
                .stream()
                .map(BotFaqMapper::toDto)
                .toList();
        BotFaqResponse response = success(I18Code.MESSAGE_BOT_FAQ_LIST_SUCCESS, locale);
        response.setBotFaqDtoList(rows);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BotFaqResponse findById(Long id, Locale locale) {
        var validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }
        BotFaq faq = botFaqRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (faq == null) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_BOT_FAQ_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }
        BotFaqResponse response = success(I18Code.MESSAGE_BOT_FAQ_RETRIEVED, locale);
        response.setBotFaqDto(BotFaqMapper.toDto(faq));
        return response;
    }

    @Override
    public BotFaqResponse create(CreateBotFaqRequest request, Locale locale, String username) {
        var validation = validator.isCreateRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }
        LocalDateTime now = LocalDateTime.now();
        BotFaq faq = new BotFaq();
        faq.setQuestion(request.getQuestion().trim());
        faq.setAnswer(request.getAnswer().trim());
        faq.setCategory(BotFaqMapper.parseCategory(request.getCategory()));
        faq.setKeywords(trimOrNull(request.getKeywords()));
        faq.setPublished(request.getPublished() == null || request.getPublished());
        faq.setUseCount(0);
        faq.setEntityStatus(EntityStatus.ACTIVE);
        faq.setCreatedAt(now);
        faq.setCreatedBy(username);
        faq.setModifiedAt(now);
        faq.setModifiedBy(username);
        BotFaq saved = auditable.create(faq, locale, username);
        botFaqRagSupport.reload();
        BotFaqResponse response = success(I18Code.MESSAGE_BOT_FAQ_CREATED, locale);
        response.setBotFaqDto(BotFaqMapper.toDto(saved));
        return response;
    }

    @Override
    public BotFaqResponse update(Long id, EditBotFaqRequest request, Locale locale, String username) {
        var idValidation = validator.isIdValid(id, locale);
        if (!idValidation.getSuccess()) {
            return failure(400, idValidation.getErrorMessages().get(0));
        }
        var validation = validator.isEditRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }
        BotFaq faq = botFaqRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (faq == null) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_BOT_FAQ_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }
        if (request.getQuestion() != null) {
            faq.setQuestion(request.getQuestion().trim());
        }
        if (request.getAnswer() != null) {
            faq.setAnswer(request.getAnswer().trim());
        }
        if (request.getCategory() != null) {
            faq.setCategory(BotFaqMapper.parseCategory(request.getCategory()));
        }
        if (request.getKeywords() != null) {
            faq.setKeywords(trimOrNull(request.getKeywords()));
        }
        if (request.getPublished() != null) {
            faq.setPublished(request.getPublished());
        }
        faq.setModifiedAt(LocalDateTime.now());
        faq.setModifiedBy(username);
        BotFaq saved = auditable.update(faq, locale, username);
        botFaqRagSupport.reload();
        BotFaqResponse response = success(I18Code.MESSAGE_BOT_FAQ_UPDATED, locale);
        response.setBotFaqDto(BotFaqMapper.toDto(saved));
        return response;
    }

    @Override
    public BotFaqResponse delete(Long id, Locale locale, String username) {
        var validation = validator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }
        BotFaq faq = botFaqRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (faq == null) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_BOT_FAQ_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }
        faq.setEntityStatus(EntityStatus.DELETED);
        faq.setModifiedAt(LocalDateTime.now());
        faq.setModifiedBy(username);
        auditable.delete(faq, locale, username);
        botFaqRagSupport.reload();
        return success(I18Code.MESSAGE_BOT_FAQ_DELETED, locale);
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BotFaqResponse success(I18Code code, Locale locale) {
        BotFaqResponse response = new BotFaqResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(code.getCode(), new String[]{}, locale));
        return response;
    }

    private BotFaqResponse failure(int statusCode, String message) {
        BotFaqResponse response = new BotFaqResponse();
        response.setSuccess(false);
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }
}
