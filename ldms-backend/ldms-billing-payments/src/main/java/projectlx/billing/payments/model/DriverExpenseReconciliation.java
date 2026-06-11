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
import projectlx.billing.payments.utils.enums.DriverExpenseReconciliationStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_expense_reconciliation")
@Getter
@Setter
@ToString
public class DriverExpenseReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_reference", nullable = false, unique = true, length = 100)
    private String expenseReference;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "transaction_currency_code", nullable = false, length = 3)
    private String transactionCurrencyCode;

    @Column(name = "base_currency_code", nullable = false, length = 3)
    private String baseCurrencyCode;

    @Column(name = "exchange_rate_snapshot_id")
    private Long exchangeRateSnapshotId;

    @Column(name = "amount_transaction", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountTransaction;

    @Column(name = "amount_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountBase;

    @Column(name = "expense_category", length = 100)
    private String expenseCategory;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DriverExpenseReconciliationStatus status = DriverExpenseReconciliationStatus.PENDING;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

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
