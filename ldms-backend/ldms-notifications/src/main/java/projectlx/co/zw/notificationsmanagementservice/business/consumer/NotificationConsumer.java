package projectlx.co.zw.notificationsmanagementservice.business.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.co.zw.notificationsmanagementservice.business.logic.api.NotificationService;
import projectlx.co.zw.notificationsmanagementservice.utils.requests.NotificationRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${notifications.rabbitmq.queue}")
    public void handleMessage(NotificationRequest request) {

        log.info("[NOTIFICATION] Received from queue eventId={} templateKey={}", request.getEventId(), request.getTemplateKey());

        try {
            notificationService.processNotification(request);
        } catch (Exception e) {
            log.error("[NOTIFICATION] Unhandled exception processing message eventId={} templateKey={} error={}", request.getEventId(), request.getTemplateKey(), e.getMessage());
            // It's important to catch exceptions here to prevent infinite retry loops.
            // The message will be dead-lettered based on the queue configuration.
        }
    }
}
