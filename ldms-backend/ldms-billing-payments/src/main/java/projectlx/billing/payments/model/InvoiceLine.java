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
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_line")
@Getter
@Setter
@ToString
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "unit_price_transaction", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPriceTransaction = BigDecimal.ZERO;

    @Column(name = "line_total_transaction", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotalTransaction = BigDecimal.ZERO;

    @Column(name = "unit_price_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPriceBase = BigDecimal.ZERO;

    @Column(name = "line_total_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotalBase = BigDecimal.ZERO;

    @Column(name = "exchange_rate_snapshot_id")
    private Long exchangeRateSnapshotId;

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
