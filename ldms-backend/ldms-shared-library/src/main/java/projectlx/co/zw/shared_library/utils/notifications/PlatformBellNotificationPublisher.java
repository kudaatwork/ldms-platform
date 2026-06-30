package projectlx.co.zw.shared_library.utils.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import projectlx.co.zw.shared_library.utils.requests.PlatformBellNotificationRequest;

import java.util.UUID;

public class PlatformBellNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PlatformBellNotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(PlatformBellNotificationPublisher.class);

    public static final String EXCHANGE = "notifications.direct";
    public static final String ROUTING_KEY = "platform.notifications.bell";

    public void publish(PlatformBellNotificationRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            return;
        }
        if (request.getEventId() == null || request.getEventId().isBlank()) {
            request.setEventId(UUID.randomUUID().toString());
        }
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
            log.info("[PLATFORM-BELL] Published eventKey={} userId={} entityType={} entityId={}",
                    request.getEventKey(), request.getUserId(), request.getEntityType(), request.getEntityId());
        } catch (Exception ex) {
            log.error("[PLATFORM-BELL] Failed eventKey={} userId={}: {}",
                    request.getEventKey(), request.getUserId(), ex.getMessage());
        }
    }
}
