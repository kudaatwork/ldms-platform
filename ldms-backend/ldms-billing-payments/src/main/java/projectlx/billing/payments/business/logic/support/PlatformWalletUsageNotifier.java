package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.billing.payments.clients.OrganizationManagementServiceClient;
import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.model.PlatformWallet;
import projectlx.billing.payments.model.UsageChargeRecord;
import projectlx.co.zw.shared_library.utils.requests.NotificationRequest;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformWalletUsageNotifier {

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";
    private static final String TEMPLATE_ACTION_CHARGED = "PLATFORM_ACTION_CHARGED";
    private static final String TEMPLATE_LOW_BALANCE = "PLATFORM_WALLET_LOW_BALANCE";

    private final RabbitTemplate rabbitTemplate;
    private final OrganizationManagementServiceClient organizationManagementServiceClient;

    public void notifyUsageCharge(
            UsageChargeRecord record,
            PlatformWallet wallet,
            OrganizationBillingSetting setting) {
        if (record == null || record.getOrganizationId() == null) {
            return;
        }
        String email = resolveOrganizationEmail(record.getOrganizationId());
        if (!StringUtils.hasText(email)) {
            return;
        }

        String currency = wallet != null && wallet.getCurrencyCode() != null ? wallet.getCurrencyCode() : "USD";
        long balanceAfter = wallet != null && wallet.getBalanceCents() != null ? wallet.getBalanceCents() : 0L;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("organizationName", resolveOrganizationName(record.getOrganizationId(), setting));
        data.put("email", email);
        data.put("actionCode", record.getActionCode());
        data.put("actionDisplayName", record.getActionDisplayName());
        data.put("chargeFormatted", formatMoney(record.getChargeCents(), currency));
        data.put("balanceAfterFormatted", formatMoney(balanceAfter, currency));
        data.put("deducted", Boolean.TRUE.equals(record.getDeducted()));
        data.put("tripId", record.getTripId());
        data.put("referenceType", record.getReferenceType());
        data.put("referenceId", record.getReferenceId());

        publish(TEMPLATE_ACTION_CHARGED, record.getOrganizationId(), email, data);

        long threshold = setting != null && setting.getLowBalanceThresholdCents() != null
                ? setting.getLowBalanceThresholdCents()
                : 500L;
        if (Boolean.TRUE.equals(record.getDeducted()) && balanceAfter <= threshold) {
            Map<String, Object> lowData = new LinkedHashMap<>(data);
            lowData.put("thresholdFormatted", formatMoney(threshold, currency));
            publish(TEMPLATE_LOW_BALANCE, record.getOrganizationId(), email, lowData);
        }
    }

    private void publish(String templateKey, Long organizationId, String email, Map<String, Object> data) {
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                organizationId + ":billing",
                email,
                null,
                null);
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID().toString(),
                templateKey,
                recipient,
                data,
                new NotificationRequest.Metadata("ldms-billing-payments", null));
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        } catch (Exception ex) {
            log.warn("Failed to publish {} for org {}: {}", templateKey, organizationId, ex.getMessage());
        }
    }

    private String resolveOrganizationEmail(Long organizationId) {
        try {
            OrganizationResponse response = organizationManagementServiceClient.findById(organizationId, Locale.getDefault());
            if (response != null && response.isSuccess() && response.getOrganizationDto() != null) {
                if (StringUtils.hasText(response.getOrganizationDto().getEmail())) {
                    return response.getOrganizationDto().getEmail().trim();
                }
                if (StringUtils.hasText(response.getOrganizationDto().getContactPersonEmail())) {
                    return response.getOrganizationDto().getContactPersonEmail().trim();
                }
            }
        } catch (Exception ex) {
            log.warn("Could not resolve organisation email for org {}: {}", organizationId, ex.getMessage());
        }
        return null;
    }

    private String resolveOrganizationName(Long organizationId, OrganizationBillingSetting setting) {
        if (setting != null && StringUtils.hasText(setting.getOrganizationName())) {
            return setting.getOrganizationName();
        }
        try {
            OrganizationResponse response = organizationManagementServiceClient.findById(organizationId, Locale.getDefault());
            if (response != null && response.isSuccess() && response.getOrganizationDto() != null) {
                return response.getOrganizationDto().getName();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "Organisation";
    }

    private static String formatMoney(Long cents, String currency) {
        long value = cents != null ? cents : 0L;
        BigDecimal major = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return currency + " " + major.toPlainString();
    }
}
