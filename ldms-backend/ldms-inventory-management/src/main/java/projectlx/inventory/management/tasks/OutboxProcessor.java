package projectlx.inventory.management.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.model.OutboxEvent;
import projectlx.inventory.management.model.OutboxStatus;
import projectlx.inventory.management.repository.OutboxEventRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${outbox.processor.batchSize:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${outbox.processor.fixedDelay:5000}")
    @Transactional
    public void processPendingEvents() {
        try {
            List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, batchSize));
            if (events.isEmpty()) {
                return;
            }
            for (OutboxEvent event : events) {
                try {
                    JsonNode node = objectMapper.readTree(event.getPayload());
                    String exchange = node.path("exchange").asText();
                    String routingKey = node.path("routingKey").asText();
                    JsonNode messageNode = node.path("message");

                    Object message;
                    if (messageNode.isNull() || messageNode.isMissingNode()) {
                        message = null;
                    } else {
                        // Deserialize to generic Map to preserve structure
                        message = objectMapper.convertValue(messageNode, Object.class);
                    }

                    rabbitTemplate.convertAndSend(exchange, routingKey, message);

                    // Mark as SENT
                    event.setStatus(OutboxStatus.SENT);
                    outboxEventRepository.save(event);
                } catch (Exception e) {
                    log.error("Failed to process outbox event id={} type={} - will retry later", event.getId(), event.getEventType(), e);
                    // Leave as PENDING for retry
                }
            }
        } catch (Exception e) {
            log.error("Outbox processing loop failed", e);
        }
    }
}
