package projectlx.messaging.inbound.business.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.messaging.inbound.business.auditable.api.BotFaqServiceAuditable;
import projectlx.messaging.inbound.business.auditable.api.BotSessionServiceAuditable;
import projectlx.messaging.inbound.business.auditable.impl.BotFaqServiceAuditableImpl;
import projectlx.messaging.inbound.business.auditable.impl.BotSessionServiceAuditableImpl;
import projectlx.messaging.inbound.business.logic.api.BotAnalyticsService;
import projectlx.messaging.inbound.business.logic.api.BotFaqService;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeService;
import projectlx.messaging.inbound.business.logic.api.BotSessionService;
import projectlx.messaging.inbound.business.logic.impl.BotAnalyticsServiceImpl;
import projectlx.messaging.inbound.business.logic.impl.BotFaqServiceImpl;
import projectlx.messaging.inbound.business.logic.impl.BotKnowledgeServiceImpl;
import projectlx.messaging.inbound.business.logic.impl.BotSessionServiceImpl;
import projectlx.messaging.inbound.business.logic.support.BotCallerProfileSupport;
import projectlx.messaging.inbound.business.logic.support.BotFaqRagSupport;
import projectlx.messaging.inbound.business.logic.support.BotSessionMapper;
import projectlx.messaging.inbound.business.logic.support.GeminiLlmClient;
import projectlx.messaging.inbound.business.logic.support.LdmsKnowledgeContextSupport;
import projectlx.messaging.inbound.business.validator.api.BotFaqServiceValidator;
import projectlx.messaging.inbound.business.validator.api.BotSessionServiceValidator;
import projectlx.messaging.inbound.business.validator.impl.BotFaqServiceValidatorImpl;
import projectlx.messaging.inbound.business.validator.impl.BotSessionServiceValidatorImpl;
import projectlx.messaging.inbound.clients.OrganizationManagementServiceClient;
import projectlx.messaging.inbound.clients.UserManagementServiceClient;
import projectlx.messaging.inbound.repository.BotFaqRepository;
import projectlx.messaging.inbound.repository.BotMessageRepository;
import projectlx.messaging.inbound.repository.BotSessionRepository;
import projectlx.messaging.inbound.utils.config.BotKnowledgeProperties;

@Configuration
@EnableConfigurationProperties(BotKnowledgeProperties.class)
public class BusinessConfig {

    @Bean
    public LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport(
            ResourcePatternResolver resourcePatternResolver,
            ResourceLoader resourceLoader,
            BotKnowledgeProperties botKnowledgeProperties) {
        return new LdmsKnowledgeContextSupport(resourcePatternResolver, resourceLoader, botKnowledgeProperties);
    }

    @Bean
    public BotFaqRagSupport botFaqRagSupport(BotFaqRepository botFaqRepository) {
        return new BotFaqRagSupport(botFaqRepository);
    }

    @Bean
    public BotKnowledgeService botKnowledgeService(LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport,
                                                   BotFaqRagSupport botFaqRagSupport) {
        return new BotKnowledgeServiceImpl(ldmsKnowledgeContextSupport, botFaqRagSupport);
    }

    @Bean
    public GeminiLlmClient geminiLlmClient(ObjectMapper objectMapper, Environment env) {
        String apiKey = env.getProperty("ldms.bot.gemini.api-key", "");
        String model = env.getProperty("ldms.bot.gemini.model", "gemini-2.5-flash");
        boolean enabled = Boolean.parseBoolean(env.getProperty("ldms.bot.gemini.enabled", "true"));
        return new GeminiLlmClient(objectMapper, apiKey, model, enabled);
    }

    @Bean
    public BotCallerProfileSupport botCallerProfileSupport(
            UserManagementServiceClient userManagementServiceClient,
            OrganizationManagementServiceClient organizationManagementServiceClient) {
        return new BotCallerProfileSupport(userManagementServiceClient, organizationManagementServiceClient);
    }

    @Bean
    public BotSessionMapper botSessionMapper(BotMessageRepository botMessageRepository) {
        return new BotSessionMapper(botMessageRepository);
    }

    @Bean
    public BotSessionServiceValidator botSessionServiceValidator(MessageService messageService) {
        return new BotSessionServiceValidatorImpl(messageService);
    }

    @Bean
    public BotSessionServiceAuditable botSessionServiceAuditable(BotSessionRepository botSessionRepository,
                                                                 BotMessageRepository botMessageRepository) {
        return new BotSessionServiceAuditableImpl(botSessionRepository, botMessageRepository);
    }

    @Bean
    public BotFaqServiceValidator botFaqServiceValidator(MessageService messageService) {
        return new BotFaqServiceValidatorImpl(messageService);
    }

    @Bean
    public BotFaqServiceAuditable botFaqServiceAuditable(BotFaqRepository botFaqRepository) {
        return new BotFaqServiceAuditableImpl(botFaqRepository);
    }

    @Bean
    public BotFaqService botFaqService(BotFaqServiceValidator botFaqServiceValidator,
                                       BotFaqServiceAuditable botFaqServiceAuditable,
                                       BotFaqRepository botFaqRepository,
                                       BotFaqRagSupport botFaqRagSupport,
                                       MessageService messageService) {
        return new BotFaqServiceImpl(
                botFaqServiceValidator,
                botFaqServiceAuditable,
                botFaqRepository,
                botFaqRagSupport,
                messageService);
    }

    @Bean
    public BotAnalyticsService botAnalyticsService(BotSessionRepository botSessionRepository,
                                                   BotMessageRepository botMessageRepository,
                                                   BotFaqRepository botFaqRepository,
                                                   MessageService messageService) {
        return new BotAnalyticsServiceImpl(
                botSessionRepository,
                botMessageRepository,
                botFaqRepository,
                messageService);
    }

    @Bean
    public BotSessionService botSessionService(BotSessionRepository botSessionRepository,
                                               BotMessageRepository botMessageRepository,
                                               BotSessionServiceAuditable botSessionServiceAuditable,
                                               BotSessionServiceValidator botSessionServiceValidator,
                                               BotCallerProfileSupport botCallerProfileSupport,
                                               BotSessionMapper botSessionMapper,
                                               LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport,
                                               BotFaqRagSupport botFaqRagSupport,
                                               GeminiLlmClient geminiLlmClient,
                                               MessageService messageService) {
        return new BotSessionServiceImpl(
                botSessionRepository,
                botMessageRepository,
                botSessionServiceAuditable,
                botSessionServiceValidator,
                botCallerProfileSupport,
                botSessionMapper,
                ldmsKnowledgeContextSupport,
                botFaqRagSupport,
                geminiLlmClient,
                messageService);
    }
}
