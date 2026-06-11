package projectlx.billing.payments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.billing.payments.utils.enums.ExchangeRateSource;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rate_snapshot")
@Getter
@Setter
@ToString
public class ExchangeRateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_rate_id")
    private Long exchangeRateId;

    @Column(name = "from_currency_code", nullable = false, length = 3)
    private String fromCurrencyCode;

    @Column(name = "to_currency_code", nullable = false, length = 3)
    private String toCurrencyCode;

    @Column(name = "rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private ExchangeRateSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by")
    private String modifiedBy;
}
