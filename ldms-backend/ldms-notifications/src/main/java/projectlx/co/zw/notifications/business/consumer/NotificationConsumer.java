package projectlx.co.zw.notifications.business.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.co.zw.notifications.business.logic.api.NotificationService;
import projectlx.co.zw.notifications.utils.exception.TemplateNotFoundException;
import projectlx.co.zw.notifications.utils.requests.NotificationRequest;

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
        } catch (TemplateNotFoundException e) {
            log.error("[NOTIFICATION] Template not found eventId={} templateKey={} error={}",
                    request.getEventId(), request.getTemplateKey(), e.getMessage());
        } catch (Exception e) {
            log.error("[NOTIFICATION] Unhandled exception processing message eventId={} templateKey={} error={}",
                    request.getEventId(), request.getTemplateKey(), e.getMessage(), e);
        }
    }
}
