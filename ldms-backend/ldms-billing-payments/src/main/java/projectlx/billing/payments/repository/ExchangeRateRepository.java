package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.billing.payments.model.ExchangeRate;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    List<ExchangeRate> findByEntityStatusNotOrderByEffectiveFromDesc(EntityStatus entityStatus);

    @Query("""
            SELECT er FROM ExchangeRate er
            WHERE er.fromCurrencyCode = :fromCurrency
              AND er.toCurrencyCode = :toCurrency
              AND er.entityStatus <> :deleted
              AND er.effectiveFrom <= :at
              AND (er.effectiveTo IS NULL OR er.effectiveTo > :at)
            ORDER BY er.effectiveFrom DESC
            """)
    Optional<ExchangeRate> findEffectiveRate(
            @Param("fromCurrency") String fromCurrency,
            @Param("toCurrency") String toCurrency,
            @Param("at") LocalDateTime at,
            @Param("deleted") EntityStatus deleted);

    Optional<ExchangeRate> findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveToIsNullAndEntityStatusNotOrderByEffectiveFromDesc(
            String fromCurrencyCode, String toCurrencyCode, EntityStatus entityStatus);
}
