package projectlx.inventory.management.business.logic.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.logic.api.OutboxService;
import projectlx.inventory.management.model.OutboxEvent;
import projectlx.inventory.management.repository.OutboxEventRepository;
import projectlx.inventory.management.utils.messaging.OutboxMessageEnvelope;

@Slf4j
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void writeEvent(String aggregateType, String aggregateId, String eventType, String exchange, String routingKey, Object payload) {

        try {
            OutboxMessageEnvelope envelope = new OutboxMessageEnvelope(exchange, routingKey, payload);
            String json = objectMapper.writeValueAsString(envelope);

            OutboxEvent event = new OutboxEvent();
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            event.setPayload(json);

            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox payload for eventType={} aggregateType={} aggregateId={}", eventType, aggregateType, aggregateId, e);
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
