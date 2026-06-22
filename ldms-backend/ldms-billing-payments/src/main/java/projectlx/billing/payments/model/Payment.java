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
import projectlx.billing.payments.utils.enums.GatewayProvider;
import projectlx.billing.payments.utils.enums.PaymentProofSource;
import projectlx.billing.payments.utils.enums.PaymentRecordStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@ToString
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 50)
    private String paymentReference;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "transaction_currency_code", nullable = false, length = 3)
    private String transactionCurrencyCode;

    @Column(name = "base_currency_code", nullable = false, length = 3)
    private String baseCurrencyCode;

    @Column(name = "exchange_rate_snapshot_id")
    private Long exchangeRateSnapshotId;

    @Column(name = "invoice_exchange_rate_snapshot_id")
    private Long invoiceExchangeRateSnapshotId;

    @Column(name = "amount_transaction", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountTransaction;

    @Column(name = "amount_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountBase;

    @Column(name = "amount_functional_at_origination", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountFunctionalAtOrigination = BigDecimal.ZERO;

    @Column(name = "realized_fx_gain_loss", nullable = false, precision = 19, scale = 4)
    private BigDecimal realizedFxGainLoss = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentRecordStatus status = PaymentRecordStatus.COMPLETED;

    @Column(name = "notes", length = 500)
    private String notes;

    // === UPLOAD / GATEWAY FIELDS ===
    @Column(name = "payment_reference_number", length = 100)
    private String paymentReferenceNumber;

    @Column(name = "proof_document_id")
    private Long proofDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_source", length = 30)
    private PaymentProofSource proofSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_provider", length = 50)
    private GatewayProvider gatewayProvider;

    // === VERIFICATION FIELDS ===
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "current_verification_stage", nullable = false)
    private Integer currentVerificationStage = 0;

    @Column(name = "required_verification_stages")
    private Integer requiredVerificationStages;

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
