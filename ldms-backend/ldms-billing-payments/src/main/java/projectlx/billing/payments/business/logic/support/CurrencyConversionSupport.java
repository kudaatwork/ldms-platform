package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import projectlx.billing.payments.model.ExchangeRate;
import projectlx.billing.payments.model.ExchangeRateSnapshot;
import projectlx.billing.payments.repository.ExchangeRateRepository;
import projectlx.billing.payments.repository.ExchangeRateSnapshotRepository;
import projectlx.billing.payments.utils.dtos.ConversionResultDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Applies accounting-grade currency conversion: rates are locked via immutable snapshots
 * at transaction time. Subsequent rate changes do not alter historical amounts.
 */
@Component
@RequiredArgsConstructor
public class CurrencyConversionSupport {

    private static final int MONEY_SCALE = 4;

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;
    private final OrganizationCurrencySupport organizationCurrencySupport;

    public ConversionResultDto convertAndLock(
            String fromCurrencyCode,
            String toCurrencyCode,
            BigDecimal amount,
            LocalDateTime at,
            String username) {
        return convertAndLockAt(fromCurrencyCode, toCurrencyCode, amount, at, username);
    }

    /**
     * IFRS 21: use the spot rate effective on the transaction date (start of day).
     */
    public ConversionResultDto convertAndLockOnDate(
            String fromCurrencyCode,
            String toCurrencyCode,
            BigDecimal amount,
            LocalDate transactionDate,
            String username) {
        LocalDate effectiveDate = transactionDate == null ? LocalDate.now() : transactionDate;
        return convertAndLockAt(
                fromCurrencyCode,
                toCurrencyCode,
                amount,
                effectiveDate.atStartOfDay(),
                username);
    }

    public ConversionResultDto convertAndLockForOrganization(
            Long organizationId,
            String transactionCurrencyCode,
            BigDecimal amount,
            LocalDate transactionDate,
            String username) {
        String transactionCurrency = normalizeCode(transactionCurrencyCode);
        String functionalCurrency = organizationCurrencySupport.resolveFunctionalCurrencyCode(organizationId, null);
        return convertAndLockOnDate(transactionCurrency, functionalCurrency, amount, transactionDate, username);
    }

    private ConversionResultDto convertAndLockAt(
            String fromCurrencyCode,
            String toCurrencyCode,
            BigDecimal amount,
            LocalDateTime at,
            String username) {

        String from = normalizeCode(fromCurrencyCode);
        String to = normalizeCode(toCurrencyCode);
        BigDecimal sourceAmount = amount == null ? BigDecimal.ZERO : amount;

        if (from.equals(to)) {
            return buildResult(from, to, sourceAmount, sourceAmount, BigDecimal.ONE, null);
        }

        ExchangeRate effectiveRate = resolveEffectiveRate(from, to, at)
                .orElseThrow(() -> new IllegalStateException(
                        "No exchange rate found for " + from + " -> " + to + " at " + at));

        ExchangeRateSnapshot snapshot = createSnapshot(effectiveRate, at, username);
        BigDecimal converted = sourceAmount.multiply(effectiveRate.getRate()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return buildResult(from, to, sourceAmount, converted, effectiveRate.getRate(), snapshot.getId());
    }

    public String resolveBaseCurrencyForCountry(Long countryId) {
        return organizationCurrencySupport.resolveFunctionalCurrencyCode(null, countryId);
    }

    public String resolveFunctionalCurrencyForOrganization(Long organizationId, Long countryId) {
        return organizationCurrencySupport.resolveFunctionalCurrencyCode(organizationId, countryId);
    }

    public Optional<ExchangeRate> resolveEffectiveRate(String fromCurrencyCode, String toCurrencyCode, LocalDateTime at) {
        String from = normalizeCode(fromCurrencyCode);
        String to = normalizeCode(toCurrencyCode);
        LocalDateTime effectiveAt = at == null ? LocalDateTime.now() : at;

        if (from.equals(to)) {
            return Optional.empty();
        }

        Optional<ExchangeRate> direct = exchangeRateRepository.findEffectiveRate(from, to, effectiveAt, EntityStatus.DELETED);
        if (direct.isPresent()) {
            return direct;
        }

        Optional<ExchangeRate> inverse = exchangeRateRepository.findEffectiveRate(to, from, effectiveAt, EntityStatus.DELETED);
        if (inverse.isEmpty() || inverse.get().getRate().compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        ExchangeRate inverted = inverse.get();
        ExchangeRate synthetic = new ExchangeRate();
        synthetic.setId(inverted.getId());
        synthetic.setFromCurrencyCode(from);
        synthetic.setToCurrencyCode(to);
        synthetic.setRate(BigDecimal.ONE.divide(inverted.getRate(), 8, RoundingMode.HALF_UP));
        synthetic.setEffectiveFrom(inverted.getEffectiveFrom());
        synthetic.setEffectiveTo(inverted.getEffectiveTo());
        synthetic.setSource(inverted.getSource());
        return Optional.of(synthetic);
    }

    public ExchangeRateSnapshot createSnapshot(ExchangeRate rate, LocalDateTime at, String username) {
        ExchangeRateSnapshot snapshot = new ExchangeRateSnapshot();
        snapshot.setExchangeRateId(rate.getId());
        snapshot.setFromCurrencyCode(rate.getFromCurrencyCode());
        snapshot.setToCurrencyCode(rate.getToCurrencyCode());
        snapshot.setRate(rate.getRate());
        snapshot.setEffectiveAt(at == null ? LocalDateTime.now() : at);
        snapshot.setSource(rate.getSource());
        snapshot.setEntityStatus(EntityStatus.ACTIVE);
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setCreatedBy(username);
        return exchangeRateSnapshotRepository.save(snapshot);
    }

    private ConversionResultDto buildResult(
            String from,
            String to,
            BigDecimal sourceAmount,
            BigDecimal convertedAmount,
            BigDecimal rateUsed,
            Long snapshotId) {
        ConversionResultDto dto = new ConversionResultDto();
        dto.setFromCurrencyCode(from);
        dto.setToCurrencyCode(to);
        dto.setSourceAmount(sourceAmount);
        dto.setConvertedAmount(convertedAmount);
        dto.setExchangeRateUsed(rateUsed);
        dto.setExchangeRateSnapshotId(snapshotId);
        return dto;
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
