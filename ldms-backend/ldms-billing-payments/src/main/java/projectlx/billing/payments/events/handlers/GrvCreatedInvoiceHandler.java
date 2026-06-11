package projectlx.billing.payments.events.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import projectlx.billing.payments.business.logic.api.InvoiceService;
import projectlx.billing.payments.utils.config.RabbitMQConsumerConfig;

import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GrvCreatedInvoiceHandler {

    private final InvoiceService invoiceService;

    @RabbitListener(queues = RabbitMQConsumerConfig.GRV_CREATED_QUEUE)
    public void handleGrvCreatedEvent(Map<String, Object> event) {
        if (event == null || !event.containsKey("grvId")) {
            log.debug("GRV created event missing grvId; ignoring for billing.");
            return;
        }
        try {
            invoiceService.generateFromGrvEvent(event, Locale.ENGLISH);
        } catch (Exception ex) {
            log.error("Failed to generate invoice from GRV event: {}", ex.getMessage(), ex);
        }
    }
}
