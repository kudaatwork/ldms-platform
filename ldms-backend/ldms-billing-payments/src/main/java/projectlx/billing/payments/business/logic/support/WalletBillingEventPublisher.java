package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import projectlx.billing.payments.utils.config.RabbitMQProducerConfig;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class WalletBillingEventPublisher {

    public static final String WALLET_DEPOSIT_CONFIRMED_ROUTING_KEY = "wallet.deposit.confirmed";

    private final RabbitTemplate rabbitTemplate;

    public void publishWalletDepositConfirmed(
            Long organizationId,
            String organizationName,
            Long depositId,
            Long transactionId,
            String receiptNumber,
            Long amountCents,
            String currencyCode) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("organizationId", organizationId);
        payload.put("organizationName", organizationName);
        payload.put("depositId", depositId);
        payload.put("transactionId", transactionId);
        payload.put("receiptNumber", receiptNumber);
        payload.put("amountCents", amountCents);
        payload.put("currencyCode", currencyCode);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQProducerConfig.BILLING_EXCHANGE,
                    WALLET_DEPOSIT_CONFIRMED_ROUTING_KEY,
                    payload);
        } catch (Exception ex) {
            log.warn("Failed to publish wallet.deposit.confirmed for org {} deposit {}: {}",
                    organizationId, depositId, ex.getMessage());
        }
    }
}
