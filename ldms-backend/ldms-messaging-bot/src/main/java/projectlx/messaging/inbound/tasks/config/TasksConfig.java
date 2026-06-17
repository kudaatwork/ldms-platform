package projectlx.messaging.inbound.tasks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeService;
import projectlx.messaging.inbound.tasks.impl.BotKnowledgeReloadTask;
import projectlx.messaging.inbound.utils.config.BotKnowledgeProperties;

@Configuration
@EnableScheduling
public class TasksConfig {

    @Bean
    public BotKnowledgeReloadTask botKnowledgeReloadTask(BotKnowledgeService botKnowledgeService,
                                                         BotKnowledgeProperties properties) {
        return new BotKnowledgeReloadTask(botKnowledgeService, properties);
    }
}
