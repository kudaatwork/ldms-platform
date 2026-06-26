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
import projectlx.messaging.inbound.business.auditable.api.BotKnowledgeDocumentServiceAuditable;
import projectlx.messaging.inbound.business.auditable.api.BotSessionServiceAuditable;
import projectlx.messaging.inbound.business.auditable.impl.BotFaqServiceAuditableImpl;
import projectlx.messaging.inbound.business.auditable.impl.BotKnowledgeDocumentServiceAuditableImpl;
import projectlx.messaging.inbound.business.auditable.impl.BotSessionServiceAuditableImpl;
import projectlx.messaging.inbound.business.logic.api.BotAnalyticsService;
import projectlx.messaging.inbound.business.logic.api.BotFaqService;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeDocumentService;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeService;
import projectlx.messaging.inbound.business.logic.api.BotLlmSettingsService;
import projectlx.messaging.inbound.business.logic.api.BotSessionService;
import projectlx.messaging.inbound.business.logic.impl.BotAnalyticsServiceImpl;
import projectlx.messaging.inbound.business.logic.impl.BotFaqServiceImpl;
import projectlx.messaging.inbound.business.logic.impl.BotKnowledgeDocumentServiceImpl;
import projectlx.messaging.inbound.business.logic.impl.BotKnowledgeServiceImpl;
import projectlx.messaging.inbound.business.logic.impl.BotLlmSettingsServiceImpl;
import projectlx.messaging.inbound.business.logic.impl.BotSessionServiceImpl;
import projectlx.messaging.inbound.business.logic.support.BotAgentLlmBridge;
import projectlx.messaging.inbound.business.logic.support.BotAgentOrchestrator;
import projectlx.messaging.inbound.business.logic.support.BotConfigPropertySupport;
import projectlx.messaging.inbound.business.logic.support.BotGuideModeSupport;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;
import projectlx.messaging.inbound.business.logic.support.agent.AddUsersToUserGroupTool;
import projectlx.messaging.inbound.business.logic.support.agent.CreateSupportTicketTool;
import projectlx.messaging.inbound.business.logic.support.agent.CreateUserGroupTool;
import projectlx.messaging.inbound.business.logic.support.agent.GetPortalNavigationTool;
import projectlx.messaging.inbound.business.logic.support.agent.GetPricingCatalogTool;
import projectlx.messaging.inbound.business.logic.support.agent.GetSessionContextTool;
import projectlx.messaging.inbound.business.logic.support.agent.GetWalletSummaryTool;
import projectlx.messaging.inbound.business.logic.support.agent.ListOrgUsersTool;
import projectlx.messaging.inbound.business.logic.support.agent.ListSupportTicketsTool;
import projectlx.messaging.inbound.business.logic.support.agent.ListUserGroupsTool;
import projectlx.messaging.inbound.business.logic.support.agent.SearchSystemKnowledgeTool;
import projectlx.messaging.inbound.business.logic.support.BotCallerProfileSupport;
import projectlx.messaging.inbound.business.logic.support.BotFaqRagSupport;
import projectlx.messaging.inbound.business.logic.support.BotKnowledgeDocumentRagSupport;
import projectlx.messaging.inbound.business.logic.support.BotPdfTextExtractorSupport;
import projectlx.messaging.inbound.business.logic.support.BotSessionMapper;
import projectlx.messaging.inbound.business.logic.support.BotLlmClient;
import projectlx.messaging.inbound.business.logic.support.BotLlmRuntimeSettings;
import projectlx.messaging.inbound.business.logic.support.BotLlmRouter;
import projectlx.messaging.inbound.business.logic.support.AnthropicLlmClient;
import projectlx.messaging.inbound.business.logic.support.GeminiLlmClient;
import projectlx.messaging.inbound.business.logic.support.BotLlmSettingsSupport;
import projectlx.messaging.inbound.business.logic.support.LdmsKnowledgeContextSupport;
import projectlx.messaging.inbound.business.validator.api.BotFaqServiceValidator;
import projectlx.messaging.inbound.business.validator.api.BotKnowledgeDocumentServiceValidator;
import projectlx.messaging.inbound.business.validator.api.BotSessionServiceValidator;
import projectlx.messaging.inbound.business.validator.impl.BotFaqServiceValidatorImpl;
import projectlx.messaging.inbound.business.validator.impl.BotKnowledgeDocumentServiceValidatorImpl;
import projectlx.messaging.inbound.business.validator.impl.BotSessionServiceValidatorImpl;
import projectlx.co.zw.shared_library.billing.PlatformWalletUsageSupport;
import projectlx.messaging.inbound.business.logic.support.BotBillingSupport;
import projectlx.messaging.inbound.business.logic.support.BotPricingSupport;
import projectlx.messaging.inbound.clients.BillingPaymentsServiceClient;
import projectlx.messaging.inbound.clients.HelpSupportServiceClient;
import projectlx.messaging.inbound.clients.OrganizationManagementServiceClient;
import projectlx.messaging.inbound.clients.UserManagementAgentClient;
import projectlx.messaging.inbound.clients.UserManagementServiceClient;
import java.util.List;
import projectlx.messaging.inbound.repository.BotFaqRepository;
import projectlx.messaging.inbound.repository.BotKnowledgeDocumentRepository;
import projectlx.messaging.inbound.repository.BotMessageRepository;
import projectlx.messaging.inbound.repository.BotSessionRepository;
import projectlx.messaging.inbound.utils.config.BotKnowledgeProperties;

@Configuration
@EnableConfigurationProperties(BotKnowledgeProperties.class)
public class BusinessConfig {

    @Bean
    public BotPricingSupport botPricingSupport(BillingPaymentsServiceClient billingPaymentsServiceClient) {
        return new BotPricingSupport(billingPaymentsServiceClient);
    }

    @Bean
    public LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport(
            ResourcePatternResolver resourcePatternResolver,
            ResourceLoader resourceLoader,
            BotKnowledgeProperties botKnowledgeProperties,
            BotPricingSupport botPricingSupport) {
        return new LdmsKnowledgeContextSupport(
                resourcePatternResolver, resourceLoader, botKnowledgeProperties, botPricingSupport);
    }

    @Bean
    public BotFaqRagSupport botFaqRagSupport(BotFaqRepository botFaqRepository) {
        return new BotFaqRagSupport(botFaqRepository);
    }

    @Bean
    public BotPdfTextExtractorSupport botPdfTextExtractorSupport() {
        return new BotPdfTextExtractorSupport();
    }

    @Bean
    public BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport(
            BotKnowledgeDocumentRepository botKnowledgeDocumentRepository) {
        return new BotKnowledgeDocumentRagSupport(botKnowledgeDocumentRepository);
    }

    @Bean
    public BotKnowledgeDocumentServiceValidator botKnowledgeDocumentServiceValidator(
            MessageService messageService) {
        return new BotKnowledgeDocumentServiceValidatorImpl(messageService);
    }

    @Bean
    public BotKnowledgeDocumentServiceAuditable botKnowledgeDocumentServiceAuditable(
            BotKnowledgeDocumentRepository botKnowledgeDocumentRepository) {
        return new BotKnowledgeDocumentServiceAuditableImpl(botKnowledgeDocumentRepository);
    }

    @Bean
    public BotKnowledgeDocumentService botKnowledgeDocumentService(
            BotKnowledgeDocumentServiceValidator botKnowledgeDocumentServiceValidator,
            BotKnowledgeDocumentServiceAuditable botKnowledgeDocumentServiceAuditable,
            BotKnowledgeDocumentRepository botKnowledgeDocumentRepository,
            BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport,
            BotPdfTextExtractorSupport botPdfTextExtractorSupport,
            MessageService messageService) {
        return new BotKnowledgeDocumentServiceImpl(
                botKnowledgeDocumentServiceValidator,
                botKnowledgeDocumentServiceAuditable,
                botKnowledgeDocumentRepository,
                botKnowledgeDocumentRagSupport,
                botPdfTextExtractorSupport,
                messageService);
    }

    @Bean
    public BotLlmSettingsService botLlmSettingsService(BotLlmSettingsSupport botLlmSettingsSupport) {
        return new BotLlmSettingsServiceImpl(botLlmSettingsSupport);
    }

    @Bean
    public BotKnowledgeService botKnowledgeService(LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport,
                                                   BotFaqRagSupport botFaqRagSupport,
                                                   BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport) {
        return new BotKnowledgeServiceImpl(ldmsKnowledgeContextSupport, botFaqRagSupport,
                botKnowledgeDocumentRagSupport);
    }

    @Bean
    public BotLlmRuntimeSettings botLlmRuntimeSettings() {
        return new BotLlmRuntimeSettings();
    }

    @Bean
    public GeminiLlmClient geminiLlmClient(ObjectMapper objectMapper,
                                             Environment env,
                                             BotLlmRuntimeSettings botLlmRuntimeSettings) {
        String apiKey = BotConfigPropertySupport.firstNonBlank(env,
                "ldms.bot.llm.gemini.api-key", "ldms.bot.gemini.api-key");
        String model = BotConfigPropertySupport.firstNonBlank(env,
                "ldms.bot.llm.gemini.model", "ldms.bot.gemini.model");
        if (model.isBlank()) {
            model = "gemini-2.5-flash";
        }
        boolean enabled = BotConfigPropertySupport.firstEnabled(env, true,
                "ldms.bot.llm.gemini.enabled", "ldms.bot.gemini.enabled");
        return new GeminiLlmClient(objectMapper, apiKey, model, enabled, botLlmRuntimeSettings);
    }

    @Bean
    public AnthropicLlmClient anthropicLlmClient(ObjectMapper objectMapper,
                                                 Environment env,
                                                 BotLlmRuntimeSettings botLlmRuntimeSettings) {
        String apiKey = BotConfigPropertySupport.firstNonBlank(env, "ldms.bot.llm.anthropic.api-key");
        String model = BotConfigPropertySupport.firstNonBlank(env, "ldms.bot.llm.anthropic.model");
        if (model.isBlank()) {
            model = "claude-sonnet-4-6";
        }
        boolean enabled = BotConfigPropertySupport.firstEnabled(env, true, "ldms.bot.llm.anthropic.enabled");
        return new AnthropicLlmClient(objectMapper, apiKey, model, enabled, botLlmRuntimeSettings);
    }

    @Bean
    public BotGuideModeSupport botGuideModeSupport(LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport,
                                                   BotFaqRagSupport botFaqRagSupport,
                                                   BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport) {
        return new BotGuideModeSupport(ldmsKnowledgeContextSupport, botFaqRagSupport, botKnowledgeDocumentRagSupport);
    }

    @Bean
    public BotLlmRouter botLlmRouter(GeminiLlmClient geminiLlmClient,
                                     AnthropicLlmClient anthropicLlmClient,
                                     BotLlmRuntimeSettings botLlmRuntimeSettings,
                                     BotGuideModeSupport botGuideModeSupport,
                                     Environment env) {
        String provider = env.getProperty("ldms.bot.llm.provider", "auto");
        return new BotLlmRouter(BotLlmRouter.ProviderMode.from(provider), botLlmRuntimeSettings,
                geminiLlmClient, anthropicLlmClient, botGuideModeSupport);
    }

    @Bean
    public BotLlmClient botLlmClient(BotLlmRouter botLlmRouter) {
        return botLlmRouter;
    }

    @Bean
    public BotLlmSettingsSupport botLlmSettingsSupport(BotLlmRouter botLlmRouter,
                                                       BotLlmRuntimeSettings botLlmRuntimeSettings,
                                                       GeminiLlmClient geminiLlmClient,
                                                       AnthropicLlmClient anthropicLlmClient,
                                                       Environment env) {
        String provider = env.getProperty("ldms.bot.llm.provider", "auto");
        return new BotLlmSettingsSupport(botLlmRouter, botLlmRuntimeSettings, geminiLlmClient,
                anthropicLlmClient, BotLlmRouter.ProviderMode.from(provider));
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
    public PlatformWalletUsageSupport platformWalletUsageSupport(BillingPaymentsServiceClient billingPaymentsServiceClient) {
        return new PlatformWalletUsageSupport(billingPaymentsServiceClient::recordUsageCharge, "ldms-messaging-bot");
    }

    @Bean
    public BotBillingSupport botBillingSupport(
            BotCallerProfileSupport botCallerProfileSupport,
            PlatformWalletUsageSupport platformWalletUsageSupport) {
        return new BotBillingSupport(botCallerProfileSupport, platformWalletUsageSupport);
    }

    @Bean
    public BotAgentLlmBridge botAgentLlmBridge(ObjectMapper objectMapper,
                                               BotLlmRouter botLlmRouter,
                                               GeminiLlmClient geminiLlmClient,
                                               AnthropicLlmClient anthropicLlmClient,
                                               Environment environment) {
        return new BotAgentLlmBridge(objectMapper, botLlmRouter, geminiLlmClient, anthropicLlmClient, environment);
    }

    @Bean
    public BotAgentToolRegistry botAgentToolRegistry(BotCallerProfileSupport botCallerProfileSupport,
                                                     BillingPaymentsServiceClient billingPaymentsServiceClient,
                                                     HelpSupportServiceClient helpSupportServiceClient,
                                                     UserManagementAgentClient userManagementAgentClient,
                                                     ObjectMapper objectMapper,
                                                     LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport,
                                                     BotFaqRagSupport botFaqRagSupport,
                                                     BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport) {
        return new BotAgentToolRegistry(List.of(
                new GetSessionContextTool(botCallerProfileSupport),
                new GetWalletSummaryTool(billingPaymentsServiceClient),
                new GetPricingCatalogTool(billingPaymentsServiceClient),
                new GetPortalNavigationTool(),
                new ListSupportTicketsTool(helpSupportServiceClient),
                new CreateSupportTicketTool(helpSupportServiceClient, objectMapper),
                new ListUserGroupsTool(userManagementAgentClient, objectMapper),
                new CreateUserGroupTool(userManagementAgentClient, objectMapper),
                new ListOrgUsersTool(userManagementAgentClient, objectMapper),
                new AddUsersToUserGroupTool(userManagementAgentClient, objectMapper),
                new SearchSystemKnowledgeTool(ldmsKnowledgeContextSupport, botFaqRagSupport, botKnowledgeDocumentRagSupport)
        ));
    }

    @Bean
    public BotAgentOrchestrator botAgentOrchestrator(BotAgentLlmBridge botAgentLlmBridge,
                                                     BotAgentToolRegistry botAgentToolRegistry,
                                                     BotLlmClient botLlmClient,
                                                     BotGuideModeSupport botGuideModeSupport,
                                                     ObjectMapper objectMapper) {
        return new BotAgentOrchestrator(botAgentLlmBridge, botAgentToolRegistry, botLlmClient, botGuideModeSupport, objectMapper);
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
                                               BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport,
                                               BotLlmClient botLlmClient,
                                               BotAgentOrchestrator botAgentOrchestrator,
                                               BotBillingSupport botBillingSupport,
                                               BotPricingSupport botPricingSupport,
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
                botKnowledgeDocumentRagSupport,
                botLlmClient,
                botAgentOrchestrator,
                botBillingSupport,
                botPricingSupport,
                messageService);
    }
}
