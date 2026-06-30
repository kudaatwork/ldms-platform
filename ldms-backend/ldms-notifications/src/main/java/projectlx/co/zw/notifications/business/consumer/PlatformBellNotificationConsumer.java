package projectlx.co.zw.notifications.business.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.co.zw.notifications.business.logic.api.PlatformUserNotificationService;
import projectlx.co.zw.shared_library.utils.requests.PlatformBellNotificationRequest;

import java.util.Locale;

@Component
public class PlatformBellNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(PlatformBellNotificationConsumer.class);

    private final PlatformUserNotificationService platformUserNotificationService;

    public PlatformBellNotificationConsumer(PlatformUserNotificationService platformUserNotificationService) {
        this.platformUserNotificationService = platformUserNotificationService;
    }

    @RabbitListener(queues = "${notifications.rabbitmq.platform-bell-queue}")
    public void handleMessage(PlatformBellNotificationRequest request) {
        log.info("[PLATFORM-BELL] Received eventKey={} userId={}",
                request != null ? request.getEventKey() : null,
                request != null ? request.getUserId() : null);
        platformUserNotificationService.ingest(request, Locale.ENGLISH);
    }
}
