package projectlx.messaging.inbound.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.messaging.inbound.business.logic.api.BotAnalyticsService;
import projectlx.messaging.inbound.business.logic.api.BotFaqService;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeService;
import projectlx.messaging.inbound.business.logic.api.BotSessionService;
import projectlx.messaging.inbound.service.processor.api.BotAnalyticsServiceProcessor;
import projectlx.messaging.inbound.service.processor.api.BotFaqServiceProcessor;
import projectlx.messaging.inbound.service.processor.api.BotKnowledgeServiceProcessor;
import projectlx.messaging.inbound.service.processor.api.BotSessionServiceProcessor;
import projectlx.messaging.inbound.service.processor.impl.BotAnalyticsServiceProcessorImpl;
import projectlx.messaging.inbound.service.processor.impl.BotFaqServiceProcessorImpl;
import projectlx.messaging.inbound.service.processor.impl.BotKnowledgeServiceProcessorImpl;
import projectlx.messaging.inbound.service.processor.impl.BotSessionServiceProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public BotKnowledgeServiceProcessor botKnowledgeServiceProcessor(BotKnowledgeService botKnowledgeService) {
        return new BotKnowledgeServiceProcessorImpl(botKnowledgeService);
    }

    @Bean
    public BotFaqServiceProcessor botFaqServiceProcessor(BotFaqService botFaqService) {
        return new BotFaqServiceProcessorImpl(botFaqService);
    }

    @Bean
    public BotAnalyticsServiceProcessor botAnalyticsServiceProcessor(BotAnalyticsService botAnalyticsService) {
        return new BotAnalyticsServiceProcessorImpl(botAnalyticsService);
    }

    @Bean
    public BotSessionServiceProcessor botSessionServiceProcessor(BotSessionService botSessionService) {
        return new BotSessionServiceProcessorImpl(botSessionService);
    }
}
