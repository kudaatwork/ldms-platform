package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.inventory.management.clients.BillingPaymentsServiceClient;
import projectlx.inventory.management.clients.dto.BillingConversionResultDto;
import projectlx.inventory.management.clients.dto.BillingCurrencyConversionResponse;
import projectlx.inventory.management.clients.dto.LockCurrencyConversionRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;

/**
 * IFRS 21: locks spot rates at transaction date via billing-payments and returns dual-currency amounts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionCurrencyConversionSupport {

    private static final int MONEY_SCALE = 4;

    private final BillingPaymentsServiceClient billingPaymentsServiceClient;

    public BillingConversionResultDto convertToFunctionalCurrency(
            Long organizationId,
            String transactionCurrencyCode,
            BigDecimal amount,
            LocalDate transactionDate) {

        if (amount == null) {
            return sameCurrencyResult(transactionCurrencyCode, transactionCurrencyCode, BigDecimal.ZERO);
        }

        LockCurrencyConversionRequest request = new LockCurrencyConversionRequest();
        request.setOrganizationId(organizationId);
        request.setTransactionCurrencyCode(transactionCurrencyCode);
        request.setAmount(amount);
        request.setTransactionDate(transactionDate == null ? LocalDate.now() : transactionDate);

        try {
            BillingCurrencyConversionResponse response = billingPaymentsServiceClient
                    .lockConversionForOrganization(request, Locale.ENGLISH);
            if (response != null && response.isSuccess() && response.getConversionResultDto() != null) {
                return response.getConversionResultDto();
            }
        } catch (Exception ex) {
            log.warn("Billing conversion failed for org {} {}: {}", organizationId, transactionCurrencyCode, ex.getMessage());
        }

        return sameCurrencyResult(transactionCurrencyCode, transactionCurrencyCode, amount);
    }

    public BigDecimal scaleByRate(BigDecimal transactionAmount, BigDecimal rate) {
        if (transactionAmount == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return transactionAmount.multiply(rate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BillingConversionResultDto sameCurrencyResult(String from, String to, BigDecimal amount) {
        BillingConversionResultDto dto = new BillingConversionResultDto();
        dto.setFromCurrencyCode(from);
        dto.setToCurrencyCode(to);
        dto.setSourceAmount(amount);
        dto.setConvertedAmount(amount == null ? BigDecimal.ZERO : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        dto.setExchangeRateUsed(BigDecimal.ONE);
        dto.setExchangeRateSnapshotId(null);
        return dto;
    }
}
