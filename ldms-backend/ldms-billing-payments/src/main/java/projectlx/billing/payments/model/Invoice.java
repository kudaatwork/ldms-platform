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
import projectlx.billing.payments.utils.enums.InvoiceSourceType;
import projectlx.billing.payments.utils.enums.InvoiceStatus;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@ToString
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private InvoiceSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "source_reference", length = 100)
    private String sourceReference;

    @Column(name = "grv_id")
    private Long grvId;

    @Column(name = "grv_number", length = 50)
    private String grvNumber;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Column(name = "purchase_order_number", length = 50)
    private String purchaseOrderNumber;

    @Column(name = "transaction_currency_code", nullable = false, length = 3)
    private String transactionCurrencyCode;

    @Column(name = "base_currency_code", nullable = false, length = 3)
    private String baseCurrencyCode;

    @Column(name = "exchange_rate_snapshot_id")
    private Long exchangeRateSnapshotId;

    @Column(name = "subtotal_transaction", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotalTransaction = BigDecimal.ZERO;

    @Column(name = "subtotal_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotalBase = BigDecimal.ZERO;

    @Column(name = "tax_transaction", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxTransaction = BigDecimal.ZERO;

    @Column(name = "tax_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxBase = BigDecimal.ZERO;

    @Column(name = "total_transaction", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalTransaction = BigDecimal.ZERO;

    @Column(name = "total_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalBase = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_term", length = 50)
    private PaymentTerm paymentTerm;

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

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
