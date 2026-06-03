package projectlx.co.zw.notifications.service.tasks;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import projectlx.co.zw.notifications.service.processor.api.NotificationLogProcessor;

@Component
@RequiredArgsConstructor
public class NotificationLogRetentionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationLogRetentionScheduler.class);

    private final NotificationLogProcessor notificationLogProcessor;

    @Value("${notifications.log.retention-days:90}")
    private int retentionDays;

    @Scheduled(
            fixedDelayString = "${notifications.log.churn.fixed-delay-ms:7776000000}",
            initialDelayString = "${notifications.log.churn.initial-delay-ms:7776000000}")
    public void churnOutExpiredLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(Math.max(retentionDays, 1));
        int deleted = notificationLogProcessor.churnOutLogsBefore(cutoff, "SYSTEM_SCHEDULER");
        logger.info("Notification log retention churn completed. cutoff={}, deleted={}", cutoff, deleted);
    }
}
