package projectlx.messaging.inbound.business.logic.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.messaging.inbound.business.auditable.api.BotSessionServiceAuditable;
import projectlx.messaging.inbound.business.logic.api.BotSessionService;
import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentOrchestrator;
import projectlx.messaging.inbound.business.logic.support.BotBillingSupport;
import projectlx.messaging.inbound.business.logic.support.BotCallerProfileSupport;
import projectlx.messaging.inbound.business.logic.support.BotPricingSupport;
import projectlx.messaging.inbound.business.logic.support.BotResponseSanitizer;
import projectlx.messaging.inbound.business.logic.support.BotSessionMapper;
import projectlx.messaging.inbound.business.logic.support.BotLlmFallbackSupport;
import projectlx.messaging.inbound.business.logic.support.BotLlmHistorySupport;
import projectlx.messaging.inbound.business.logic.support.BotLlmClient;
import projectlx.messaging.inbound.business.logic.support.BotFaqRagSupport;
import projectlx.messaging.inbound.business.logic.support.BotKnowledgeDocumentRagSupport;
import projectlx.messaging.inbound.business.logic.support.LdmsKnowledgeContextSupport;
import projectlx.messaging.inbound.business.logic.support.LexiBotPersonality;
import projectlx.messaging.inbound.business.validator.api.BotSessionServiceValidator;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.model.BotSession;
import projectlx.messaging.inbound.repository.BotMessageRepository;
import projectlx.messaging.inbound.repository.BotSessionRepository;
import projectlx.messaging.inbound.utils.dtos.BotPricingDto;
import projectlx.messaging.inbound.utils.dtos.BotSessionDto;
import projectlx.messaging.inbound.utils.enums.BotAssistantMode;
import projectlx.messaging.inbound.utils.enums.BotChannel;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;
import projectlx.messaging.inbound.utils.enums.BotSessionStatus;
import projectlx.messaging.inbound.utils.enums.I18Code;
import projectlx.messaging.inbound.utils.requests.RateBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.SendBotMessageRequest;
import projectlx.messaging.inbound.utils.requests.StartBotSessionRequest;
import projectlx.messaging.inbound.utils.requests.UpdateBotAssistantModeRequest;
import projectlx.messaging.inbound.utils.responses.BotSessionResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Transactional
public class BotSessionServiceImpl implements BotSessionService {

    private static final String GUEST_USERNAME_PREFIX = "guest:";

    private final BotSessionRepository botSessionRepository;
    private final BotMessageRepository botMessageRepository;
    private final BotSessionServiceAuditable botSessionServiceAuditable;
    private final BotSessionServiceValidator botSessionServiceValidator;
    private final BotCallerProfileSupport botCallerProfileSupport;
    private final BotSessionMapper botSessionMapper;
    private final LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport;
    private final BotFaqRagSupport botFaqRagSupport;
    private final BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport;
    private final BotLlmClient botLlmClient;
    private final BotAgentOrchestrator botAgentOrchestrator;
    private final BotBillingSupport botBillingSupport;
    private final BotPricingSupport botPricingSupport;
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
                                 BotLlmClient botLlmClient,
                                 BotAgentOrchestrator botAgentOrchestrator,
                                 BotBillingSupport botBillingSupport,
                                 BotPricingSupport botPricingSupport,
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
        this.botLlmClient = botLlmClient;
        this.botAgentOrchestrator = botAgentOrchestrator;
        this.botBillingSupport = botBillingSupport;
        this.botPricingSupport = botPricingSupport;
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
        BotAssistantMode assistantMode = BotAssistantMode.from(request.getAssistantMode());

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
                : LexiBotPersonality.DEFAULT_TOPIC);
        session.setAssistantMode(assistantMode);
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
        String greetingBody = assistantMode == BotAssistantMode.AGENT
                ? LexiBotPersonality.agentGreeting(profile.displayName())
                : LexiBotPersonality.assistantGreeting(profile.displayName());
        greeting.setBody(greetingBody);
        greeting.setEntityStatus(EntityStatus.ACTIVE);
        greeting.setCreatedAt(now);
        greeting.setCreatedBy("ldms-bot");
        botSessionServiceAuditable.createMessage(greeting, locale, username);

        return success(I18Code.MESSAGE_BOT_SESSION_CREATED, locale, botSessionMapper.toDto(saved, true),
                botPricingSupport.currentPricing());
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

        if (request.getAssistantMode() != null && !request.getAssistantMode().isBlank()) {
            BotAssistantMode requestedMode = BotAssistantMode.from(request.getAssistantMode());
            if (session.getAssistantMode() != requestedMode) {
                session.setAssistantMode(requestedMode);
                session.setModifiedAt(LocalDateTime.now());
                session.setModifiedBy(username);
                botSessionServiceAuditable.updateSession(session, locale, username);
            }
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
                || LexiBotPersonality.LEGACY_TOPIC.equalsIgnoreCase(session.getTopic())
                || LexiBotPersonality.LEGACY_TOPIC_CHAT.equalsIgnoreCase(session.getTopic())
                || LexiBotPersonality.DEFAULT_TOPIC.equalsIgnoreCase(session.getTopic())) {
            session.setTopic(summarizeTopic(userBody));
        }

        List<BotMessage> history = botMessageRepository.findByBotSessionIdAndEntityStatusOrderByCreatedAtAsc(
                session.getId(), EntityStatus.ACTIVE);
        BotAssistantMode mode = session.getAssistantMode() != null ? session.getAssistantMode() : BotAssistantMode.ASSISTANT;
        String systemPrompt = ldmsKnowledgeContextSupport.systemPrompt(
                session.getUserDisplayName(), session.getOrganizationName(), userBody, mode,
                botFaqRagSupport, botKnowledgeDocumentRagSupport);
        String botReply;
        if (BotLlmFallbackSupport.isAccuracyChallengeQuery(userBody)) {
            botReply = BotLlmFallbackSupport.accuracyChallengeReplyFor();
        } else {
            BotCallerProfileSupport.CallerProfile profile = botCallerProfileSupport.resolve(username);
            BotAgentExecutionContext agentContext = BotAgentOrchestrator.contextFrom(username, profile, locale, mode);
            botReply = botAgentOrchestrator.run(systemPrompt, history, agentContext);
        }
        botReply = BotResponseSanitizer.forUserDisplay(botReply);

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

    @Override
    public BotSessionResponse updateAssistantMode(UpdateBotAssistantModeRequest request, Locale locale, String username) {
        var validation = botSessionServiceValidator.isUpdateAssistantModeRequestValid(request, locale);
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

        session.setAssistantMode(BotAssistantMode.from(request.getAssistantMode()));
        session.setModifiedAt(LocalDateTime.now());
        session.setModifiedBy(username);
        botSessionServiceAuditable.updateSession(session, locale, username);

        return success(I18Code.MESSAGE_BOT_ASSISTANT_MODE_UPDATED, locale, botSessionMapper.toDto(session, true));
    }

    private static String summarizeTopic(String userBody) {
        String trimmed = userBody.trim();
        if (trimmed.length() <= 80) {
            return trimmed;
        }
        return trimmed.substring(0, 77) + "...";
    }

    @Override
    @Transactional(readOnly = true)
    public BotSessionResponse getPricing(Locale locale) {
        return pricingSuccess(I18Code.MESSAGE_BOT_PRICING_RETRIEVED, locale, botPricingSupport.currentPricing());
    }

    @Override
    public BotSessionResponse startGuestSession(StartBotSessionRequest request, Locale locale) {
        var validation = botSessionServiceValidator.isStartSessionRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return failure(400, validation.getErrorMessages().get(0));
        }

        LocalDateTime now = LocalDateTime.now();
        String sessionId = BotSessionMapper.newSessionPublicId();
        String guestUsername = guestUsernameFor(sessionId);

        BotSession session = new BotSession();
        session.setSessionId(sessionId);
        session.setRequesterUsername(guestUsername);
        session.setUserDisplayName("Visitor");
        session.setUserPhone("");
        session.setOrganizationId(null);
        session.setOrganizationName("Project LX");
        session.setChannel(BotChannel.WEB);
        session.setStatus(BotSessionStatus.ACTIVE);
        session.setTopic(resolveGuestTopic(request));
        session.setAssistantMode(BotAssistantMode.ASSISTANT);
        session.setEntityStatus(EntityStatus.ACTIVE);
        session.setCreatedAt(now);
        session.setCreatedBy(guestUsername);
        session.setModifiedAt(now);
        session.setModifiedBy(guestUsername);

        BotSession saved = botSessionServiceAuditable.createSession(session, locale, guestUsername);

        BotMessage greeting = new BotMessage();
        greeting.setBotSession(saved);
        greeting.setRole(BotMessageRole.BOT);
        greeting.setBody(isGuestLiveChatSession(saved)
                ? LexiBotPersonality.landingLiveChatGreeting()
                : LexiBotPersonality.landingGuestGreeting());
        greeting.setEntityStatus(EntityStatus.ACTIVE);
        greeting.setCreatedAt(now);
        greeting.setCreatedBy("ldms-bot");
        botSessionServiceAuditable.createMessage(greeting, locale, guestUsername);

        return success(I18Code.MESSAGE_BOT_SESSION_CREATED, locale, botSessionMapper.toDto(saved, true));
    }

    @Override
    public BotSessionResponse sendGuestMessage(SendBotMessageRequest request, Locale locale) {
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
        if (!isGuestSession(session)) {
            return failure(403, messageService.getMessage(I18Code.MESSAGE_BOT_ACCESS_DENIED.getCode(),
                    new String[]{}, locale));
        }

        String guestUsername = session.getRequesterUsername();
        session.setAssistantMode(BotAssistantMode.ASSISTANT);

        LocalDateTime now = LocalDateTime.now();
        String userBody = request.getBody().trim();

        BotMessage userMessage = new BotMessage();
        userMessage.setBotSession(session);
        userMessage.setRole(BotMessageRole.USER);
        userMessage.setBody(userBody);
        userMessage.setEntityStatus(EntityStatus.ACTIVE);
        userMessage.setCreatedAt(now);
        userMessage.setCreatedBy(guestUsername);
        botSessionServiceAuditable.createMessage(userMessage, locale, guestUsername);

        List<BotMessage> history = botMessageRepository.findByBotSessionIdAndEntityStatusOrderByCreatedAtAsc(
                session.getId(), EntityStatus.ACTIVE);

        if (isGuestLiveChatSession(session)) {
            return sendGuestLiveChatReply(session, history, userMessage, locale, guestUsername);
        }

        if (session.getTopic() == null || session.getTopic().isBlank()
                || LexiBotPersonality.LEGACY_TOPIC.equalsIgnoreCase(session.getTopic())
                || LexiBotPersonality.LEGACY_TOPIC_CHAT.equalsIgnoreCase(session.getTopic())
                || LexiBotPersonality.DEFAULT_TOPIC.equalsIgnoreCase(session.getTopic())) {
            session.setTopic(summarizeTopic(userBody));
        }

        String systemPrompt = ldmsKnowledgeContextSupport.guestVisitorSystemPrompt(
                session.getUserDisplayName(), session.getOrganizationName(), userBody,
                botFaqRagSupport, botKnowledgeDocumentRagSupport);
        String botReply;
        if (BotLlmFallbackSupport.isAccuracyChallengeQuery(userBody)) {
            botReply = BotLlmFallbackSupport.accuracyChallengeReplyFor();
        } else {
            BotAgentExecutionContext agentContext = BotAgentOrchestrator.contextFrom(
                    guestUsername,
                    new BotCallerProfileSupport.CallerProfile(
                            session.getUserDisplayName(),
                            "",
                            null,
                            session.getOrganizationName(),
                            ""),
                    locale,
                    BotAssistantMode.ASSISTANT);
            botReply = botAgentOrchestrator.run(systemPrompt, history, agentContext);
        }
        botReply = BotResponseSanitizer.forUserDisplay(botReply);

        BotMessage botMessage = new BotMessage();
        botMessage.setBotSession(session);
        botMessage.setRole(BotMessageRole.BOT);
        botMessage.setBody(botReply);
        botMessage.setEntityStatus(EntityStatus.ACTIVE);
        botMessage.setCreatedAt(LocalDateTime.now());
        botMessage.setCreatedBy("ldms-bot");
        botSessionServiceAuditable.createMessage(botMessage, locale, guestUsername);

        session.setStatus(BotSessionStatus.ACTIVE);
        session.setModifiedAt(LocalDateTime.now());
        session.setModifiedBy(guestUsername);
        botSessionServiceAuditable.updateSession(session, locale, guestUsername);

        return success(I18Code.MESSAGE_BOT_MESSAGE_SENT, locale, botSessionMapper.toDto(session, true));
    }

    private BotSessionResponse sendGuestLiveChatReply(BotSession session, List<BotMessage> historyBeforeUserMsg,
                                                       BotMessage savedUserMessage, Locale locale,
                                                       String guestUsername) {
        if (session.getTopic() == null || session.getTopic().isBlank()) {
            session.setTopic(LexiBotPersonality.GUEST_LIVE_CHAT_TOPIC);
        } else if (!LexiBotPersonality.GUEST_LIVE_CHAT_TOPIC.equalsIgnoreCase(session.getTopic())) {
            session.setTopic(summarizeTopic(savedUserMessage.getBody()));
        }

        long userMessages = historyBeforeUserMsg.stream()
                .filter(m -> m.getRole() == BotMessageRole.USER)
                .count();
        if (userMessages == 1) {
            BotMessage ack = new BotMessage();
            ack.setBotSession(session);
            ack.setRole(BotMessageRole.SYSTEM);
            ack.setBody(LexiBotPersonality.liveChatMessageAcknowledgement());
            ack.setEntityStatus(EntityStatus.ACTIVE);
            ack.setCreatedAt(LocalDateTime.now());
            ack.setCreatedBy("ldms-support");
            botSessionServiceAuditable.createMessage(ack, locale, guestUsername);
        }

        session.setStatus(BotSessionStatus.ACTIVE);
        session.setModifiedAt(LocalDateTime.now());
        session.setModifiedBy(guestUsername);
        botSessionServiceAuditable.updateSession(session, locale, guestUsername);

        return success(I18Code.MESSAGE_BOT_MESSAGE_SENT, locale, botSessionMapper.toDto(session, true));
    }

    @Override
    @Transactional(readOnly = true)
    public BotSessionResponse findGuestSessionById(String sessionId, Locale locale) {
        BotSession session = botSessionRepository
                .findBySessionIdAndEntityStatus(sessionId, EntityStatus.ACTIVE)
                .orElse(null);
        if (session == null) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_BOT_SESSION_NOT_FOUND.getCode(),
                    new String[]{}, locale));
        }
        if (!isGuestSession(session)) {
            return failure(403, messageService.getMessage(I18Code.MESSAGE_BOT_ACCESS_DENIED.getCode(),
                    new String[]{}, locale));
        }
        return success(I18Code.MESSAGE_BOT_SESSION_RETRIEVED, locale, botSessionMapper.toDto(session, true));
    }

    private static String guestUsernameFor(String sessionId) {
        return GUEST_USERNAME_PREFIX + sessionId;
    }

    private static boolean isGuestSession(BotSession session) {
        return session.getRequesterUsername() != null
                && session.getRequesterUsername().startsWith(GUEST_USERNAME_PREFIX);
    }

    private static boolean isGuestLiveChatSession(BotSession session) {
        return session.getTopic() != null
                && LexiBotPersonality.GUEST_LIVE_CHAT_TOPIC.equalsIgnoreCase(session.getTopic().trim());
    }

    private static String resolveGuestTopic(StartBotSessionRequest request) {
        if (request == null || request.getTopic() == null || request.getTopic().isBlank()) {
            return LexiBotPersonality.DEFAULT_TOPIC;
        }
        String topic = request.getTopic().trim();
        if (LexiBotPersonality.GUEST_LIVE_CHAT_TOPIC.equalsIgnoreCase(topic)) {
            return LexiBotPersonality.GUEST_LIVE_CHAT_TOPIC;
        }
        if (LexiBotPersonality.DEFAULT_TOPIC.equalsIgnoreCase(topic)
                || LexiBotPersonality.LEGACY_TOPIC_CHAT.equalsIgnoreCase(topic)) {
            return LexiBotPersonality.DEFAULT_TOPIC;
        }
        return topic;
    }

    private BotSessionResponse pricingSuccess(I18Code code, Locale locale, BotPricingDto pricing) {
        BotSessionResponse response = new BotSessionResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(code.getCode(), new String[]{}, locale));
        response.setBotPricingDto(pricing);
        return response;
    }

    private BotSessionResponse success(I18Code code, Locale locale, BotSessionDto dto) {
        BotSessionResponse response = success(code, locale, dto, null);
        return response;
    }

    private BotSessionResponse success(I18Code code, Locale locale, BotSessionDto dto, BotPricingDto pricing) {
        BotSessionResponse response = new BotSessionResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(code.getCode(), new String[]{}, locale));
        response.setBotSessionDto(dto);
        if (pricing != null) {
            response.setBotPricingDto(pricing);
        }
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
