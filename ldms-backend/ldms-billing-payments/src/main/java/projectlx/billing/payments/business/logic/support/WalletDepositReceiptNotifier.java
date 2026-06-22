package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.billing.payments.clients.OrganizationManagementServiceClient;
import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.model.PlatformWallet;
import projectlx.billing.payments.model.WalletTransaction;
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
public class WalletDepositReceiptNotifier {

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";
    private static final String TEMPLATE_KEY = "WALLET_DEPOSIT_RECEIPT";

    private final RabbitTemplate rabbitTemplate;
    private final OrganizationManagementServiceClient organizationManagementServiceClient;

    public void sendWalletCreditReceipt(
            Long organizationId,
            WalletTransaction transaction,
            PlatformWallet wallet,
            OrganizationBillingSetting setting) {
        if (transaction == null || organizationId == null) {
            return;
        }
        String email = resolveOrganizationEmail(organizationId);
        if (!StringUtils.hasText(email)) {
            log.warn("Skipping wallet receipt email for org {} — organisation email unavailable", organizationId);
            return;
        }

        String receiptHtml = WalletReceiptSupport.buildReceiptHtml(transaction, wallet, setting);
        String currency = wallet != null && wallet.getCurrencyCode() != null ? wallet.getCurrencyCode() : "USD";
        long amountCents = transaction.getAmountCents() != null ? transaction.getAmountCents() : 0L;
        String orgName = wallet != null && wallet.getOrganizationName() != null
                ? wallet.getOrganizationName()
                : (setting != null ? setting.getOrganizationName() : "Organisation");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("organizationName", orgName);
        data.put("email", email);
        data.put("receiptNumber", transaction.getReceiptNumber());
        data.put("amountFormatted", formatMoney(amountCents, currency));
        data.put("balanceAfterFormatted", formatMoney(
                transaction.getBalanceAfterCents() != null ? transaction.getBalanceAfterCents() : 0L, currency));
        data.put("receiptHtml", receiptHtml);
        data.put("transactionType", transaction.getTransactionType() != null
                ? transaction.getTransactionType().name()
                : "DEPOSIT");
        data.put("description", transaction.getDescription());

        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                organizationId + ":billing",
                email,
                null,
                null);
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID().toString(),
                TEMPLATE_KEY,
                recipient,
                data,
                new NotificationRequest.Metadata("ldms-billing-payments", null));
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
            log.info("Published wallet receipt email for org {} receipt {}", organizationId, transaction.getReceiptNumber());
        } catch (Exception ex) {
            log.warn("Failed to publish wallet receipt email for org {}: {}", organizationId, ex.getMessage());
        }
    }

    private String resolveOrganizationEmail(Long organizationId) {
        try {
            OrganizationResponse response = organizationManagementServiceClient.findById(organizationId, Locale.getDefault());
            if (response != null && response.isSuccess() && response.getOrganizationDto() != null) {
                String orgEmail = response.getOrganizationDto().getEmail();
                if (StringUtils.hasText(orgEmail)) {
                    return orgEmail.trim();
                }
                String contactEmail = response.getOrganizationDto().getContactPersonEmail();
                if (StringUtils.hasText(contactEmail)) {
                    return contactEmail.trim();
                }
            }
        } catch (Exception ex) {
            log.warn("Could not resolve organisation email for org {}: {}", organizationId, ex.getMessage());
        }
        return null;
    }

    private static String formatMoney(long cents, String currency) {
        BigDecimal major = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return currency + " " + major.toPlainString();
    }
}
