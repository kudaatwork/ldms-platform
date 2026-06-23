package projectlx.messaging.inbound.business.logic.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.messaging.inbound.business.auditable.api.BotSessionServiceAuditable;
import projectlx.messaging.inbound.business.logic.api.BotSessionService;
import projectlx.messaging.inbound.business.logic.support.BotBillingSupport;
import projectlx.messaging.inbound.business.logic.support.BotCallerProfileSupport;
import projectlx.messaging.inbound.business.logic.support.BotSessionMapper;
import projectlx.messaging.inbound.business.logic.support.GeminiLlmClient;
import projectlx.messaging.inbound.business.logic.support.BotFaqRagSupport;
import projectlx.messaging.inbound.business.logic.support.BotKnowledgeDocumentRagSupport;
import projectlx.messaging.inbound.business.logic.support.LdmsKnowledgeContextSupport;
import projectlx.messaging.inbound.business.validator.api.BotSessionServiceValidator;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.model.BotSession;
import projectlx.messaging.inbound.repository.BotMessageRepository;
import projectlx.messaging.inbound.repository.BotSessionRepository;
import projectlx.messaging.inbound.utils.dtos.BotSessionDto;
import projectlx.messaging.inbound.utils.enums.BotChannel;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;
import projectlx.messaging.inbound.utils.enums.BotSessionStatus;
import projectlx.messaging.inbound.utils.enums.I18Code;
import projectlx.messaging.inbound.utils.requests.RateBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.SendBotMessageRequest;
import projectlx.messaging.inbound.utils.requests.StartBotSessionRequest;
import projectlx.messaging.inbound.utils.responses.BotSessionResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Transactional
public class BotSessionServiceImpl implements BotSessionService {

    private final BotSessionRepository botSessionRepository;
    private final BotMessageRepository botMessageRepository;
    private final BotSessionServiceAuditable botSessionServiceAuditable;
    private final BotSessionServiceValidator botSessionServiceValidator;
    private final BotCallerProfileSupport botCallerProfileSupport;
    private final BotSessionMapper botSessionMapper;
    private final LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport;
    private final BotFaqRagSupport botFaqRagSupport;
    private final BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport;
    private final GeminiLlmClient geminiLlmClient;
    private final BotBillingSupport botBillingSupport;
    private final MessageService messageService;

    public BotSessionServiceImpl(BotSessionRepository botSessionRepository,
                                 BotMessageRepository botMessageRepository,
                                 BotSessionServiceAuditable botSessionServiceAuditable,
                                 BotSessionServiceValidator botSessionServiceValidator,
                                 BotCallerProfileSupport botCallerProfileSupport,
                                 BotSessionMapper botSessionMapper,
                                 LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport,
                                 BotFaqRagSupport botFaqRagSupport,
                                 BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport,
                                 GeminiLlmClient geminiLlmClient,
                                 BotBillingSupport botBillingSupport,
                                 MessageService messageService) {
        this.botSessionRepository = botSessionRepository;
        this.botMessageRepository = botMessageRepository;
        this.botSessionServiceAuditable = botSessionServiceAuditable;
        this.botSessionServiceValidator = botSessionServiceValidator;
        this.botCallerProfileSupport = botCallerProfileSupport;
        this.botSessionMapper = botSessionMapper;
        this.ldmsKnowledgeContextSupport = ldmsKnowledgeContextSupport;
        this.botFaqRagSupport = botFaqRagSupport;
        this.botKnowledgeDocumentRagSupport = botKnowledgeDocumentRagSupport;
        this.geminiLlmClient = geminiLlmClient;
        this.botBillingSupport = botBillingSupport;
        this.messageService = messageService;
    }

    @Override
    public BotSessionResponse startSession(StartBotSessionRequest request, Locale locale, String username) {
        var validation = botSessionServiceValidator.isStartSessionRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }

        BotCallerProfileSupport.CallerProfile profile = botCallerProfileSupport.resolve(username);
        LocalDateTime now = LocalDateTime.now();

        BotSession session = new BotSession();
        session.setSessionId(BotSessionMapper.newSessionPublicId());
        session.setRequesterUsername(username);
        session.setUserDisplayName(profile.displayName());
        session.setUserPhone(profile.phone());
        session.setOrganizationId(profile.organizationId());
        session.setOrganizationName(profile.organizationName());
        session.setChannel(BotChannel.WEB);
        session.setStatus(BotSessionStatus.ACTIVE);
        session.setTopic(request.getTopic() != null && !request.getTopic().isBlank()
                ? request.getTopic().trim()
                : "LDMS assistant");
        session.setEntityStatus(EntityStatus.ACTIVE);
        session.setCreatedAt(now);
        session.setCreatedBy(username);
        session.setModifiedAt(now);
        session.setModifiedBy(username);

        BotSession saved = botSessionServiceAuditable.createSession(session, locale, username);

        botBillingSupport.chargeForSessionStart(saved, username);

        BotMessage greeting = new BotMessage();
        greeting.setBotSession(saved);
        greeting.setRole(BotMessageRole.BOT);
        greeting.setBody("""
                Hello %s! I'm the LDMS assistant. Ask me how onboarding, purchase orders, shipments, \
                trips, billing, or Help & Support work — I'll answer from the LDMS platform guide."""
                .formatted(profile.displayName()).trim());
        greeting.setEntityStatus(EntityStatus.ACTIVE);
        greeting.setCreatedAt(now);
        greeting.setCreatedBy("ldms-bot");
        botSessionServiceAuditable.createMessage(greeting, locale, username);

        return success(I18Code.MESSAGE_BOT_SESSION_CREATED, locale, botSessionMapper.toDto(saved, true));
    }

    @Override
    public BotSessionResponse sendMessage(SendBotMessageRequest request, Locale locale, String username) {
        var validation = botSessionServiceValidator.isSendMessageRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }

        BotSession session = botSessionRepository
                .findBySessionIdAndEntityStatus(request.getSessionId().trim(), EntityStatus.ACTIVE)
                .orElse(null);
        if (session == null) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_BOT_SESSION_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }
        if (!session.getRequesterUsername().equals(username)) {
            return failure(403, messageService.getMessage(I18Code.MESSAGE_BOT_ACCESS_DENIED.getCode(),
                    new String[]{}, locale));
        }

        LocalDateTime now = LocalDateTime.now();
        String userBody = request.getBody().trim();

        BotMessage userMessage = new BotMessage();
        userMessage.setBotSession(session);
        userMessage.setRole(BotMessageRole.USER);
        userMessage.setBody(userBody);
        userMessage.setEntityStatus(EntityStatus.ACTIVE);
        userMessage.setCreatedAt(now);
        userMessage.setCreatedBy(username);
        BotMessage savedUserMessage = botSessionServiceAuditable.createMessage(userMessage, locale, username);

        botBillingSupport.chargeForUserMessage(session, username, savedUserMessage.getId());

        if (session.getTopic() == null || session.getTopic().isBlank()
                || "LDMS assistant".equalsIgnoreCase(session.getTopic())) {
            session.setTopic(summarizeTopic(userBody));
        }

        List<BotMessage> history = botMessageRepository.findByBotSessionIdAndEntityStatusOrderByCreatedAtAsc(
                session.getId(), EntityStatus.ACTIVE);
        String botReply = geminiLlmClient.generateReply(
                ldmsKnowledgeContextSupport.systemPrompt(
                        session.getUserDisplayName(), session.getOrganizationName(), userBody,
                        botFaqRagSupport, botKnowledgeDocumentRagSupport),
                history);

        BotMessage botMessage = new BotMessage();
        botMessage.setBotSession(session);
        botMessage.setRole(BotMessageRole.BOT);
        botMessage.setBody(botReply);
        botMessage.setEntityStatus(EntityStatus.ACTIVE);
        botMessage.setCreatedAt(LocalDateTime.now());
        botMessage.setCreatedBy("ldms-bot");
        botSessionServiceAuditable.createMessage(botMessage, locale, username);

        session.setStatus(BotSessionStatus.ACTIVE);
        session.setModifiedAt(LocalDateTime.now());
        session.setModifiedBy(username);
        botSessionServiceAuditable.updateSession(session, locale, username);

        return success(I18Code.MESSAGE_BOT_MESSAGE_SENT, locale, botSessionMapper.toDto(session, true));
    }

    @Override
    @Transactional(readOnly = true)
    public BotSessionResponse listMySessions(Locale locale, String username) {
        List<BotSession> sessions = botSessionRepository
                .findByRequesterUsernameAndEntityStatusOrderByModifiedAtDescCreatedAtDesc(
                        username, EntityStatus.ACTIVE);
        BotSessionResponse response = success(I18Code.MESSAGE_BOT_SESSIONS_RETRIEVED, locale, null);
        response.setBotSessionDtoList(botSessionMapper.toDtoList(sessions, false));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BotSessionResponse findMySessionById(String sessionId, Locale locale, String username) {
        BotSession session = botSessionRepository
                .findBySessionIdAndEntityStatus(sessionId, EntityStatus.ACTIVE)
                .orElse(null);
        if (session == null) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_BOT_SESSION_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }
        if (!session.getRequesterUsername().equals(username)) {
            return failure(403, messageService.getMessage(I18Code.MESSAGE_BOT_ACCESS_DENIED.getCode(),
                    new String[]{}, locale));
        }
        return success(I18Code.MESSAGE_BOT_SESSION_RETRIEVED, locale, botSessionMapper.toDto(session, true));
    }

    @Override
    @Transactional(readOnly = true)
    public BotSessionResponse listAllSessions(Locale locale) {
        List<BotSession> sessions = botSessionRepository.findAllActiveSessions(EntityStatus.ACTIVE);
        BotSessionResponse response = success(I18Code.MESSAGE_BOT_SESSIONS_RETRIEVED, locale, null);
        response.setBotSessionDtoList(botSessionMapper.toDtoList(sessions, true));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BotSessionResponse findSessionById(String sessionId, Locale locale) {
        BotSession session = botSessionRepository
                .findBySessionIdAndEntityStatus(sessionId, EntityStatus.ACTIVE)
                .orElse(null);
        if (session == null) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_BOT_SESSION_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }
        return success(I18Code.MESSAGE_BOT_SESSION_RETRIEVED, locale, botSessionMapper.toDto(session, true));
    }

    @Override
    public BotSessionResponse rateSession(RateBotSessionRequest request, Locale locale, String username) {
        var validation = botSessionServiceValidator.isRateSessionRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }

        BotSession session = botSessionRepository
                .findBySessionIdAndEntityStatus(request.getSessionId().trim(), EntityStatus.ACTIVE)
                .orElse(null);
        if (session == null) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_BOT_SESSION_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }
        if (!session.getRequesterUsername().equals(username)) {
            return failure(403, messageService.getMessage(I18Code.MESSAGE_BOT_ACCESS_DENIED.getCode(),
                    new String[]{}, locale));
        }

        session.setSatisfactionScore(request.getScore());
        session.setModifiedAt(LocalDateTime.now());
        session.setModifiedBy(username);
        botSessionServiceAuditable.updateSession(session, locale, username);

        return success(I18Code.MESSAGE_BOT_SESSION_RATED, locale, botSessionMapper.toDto(session, false));
    }

    private static String summarizeTopic(String userBody) {
        String trimmed = userBody.trim();
        if (trimmed.length() <= 80) {
            return trimmed;
        }
        return trimmed.substring(0, 77) + "...";
    }

    private BotSessionResponse success(I18Code code, Locale locale, BotSessionDto dto) {
        BotSessionResponse response = new BotSessionResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(code.getCode(), new String[]{}, locale));
        response.setBotSessionDto(dto);
        return response;
    }

    private BotSessionResponse failure(int statusCode, String message) {
        BotSessionResponse response = new BotSessionResponse();
        response.setSuccess(false);
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }
}
