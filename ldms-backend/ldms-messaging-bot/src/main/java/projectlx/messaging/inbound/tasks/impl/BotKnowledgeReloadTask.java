package projectlx.messaging.inbound.tasks.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import projectlx.messaging.inbound.business.logic.api.BotKnowledgeService;
import projectlx.messaging.inbound.utils.config.BotKnowledgeProperties;

@RequiredArgsConstructor
@Slf4j
public class BotKnowledgeReloadTask {

    private final BotKnowledgeService botKnowledgeService;
    private final BotKnowledgeProperties properties;

    /** Picks up updated markdown from {@code ldms.bot.knowledge.directory} without redeploying. */
    @Scheduled(cron = "${ldms.bot.knowledge.reload-cron:0 0 */6 * * *}")
    public void scheduledReload() {
        if (properties.getReloadIntervalMinutes() <= 0 && !hasExternalDirectory()) {
            return;
        }
        reloadQuietly("scheduled");
    }

    private boolean hasExternalDirectory() {
        return properties.getDirectory() != null && !properties.getDirectory().isBlank();
    }

    private void reloadQuietly(String reason) {
        try {
            var status = botKnowledgeService.reload();
            log.info("Bot knowledge reloaded ({}) — {} document(s), {} characters",
                    reason, status.getDocumentCount(), status.getCharacterCount());
        } catch (Exception ex) {
            log.warn("Scheduled bot knowledge reload failed: {}", ex.getMessage());
        }
    }
}
