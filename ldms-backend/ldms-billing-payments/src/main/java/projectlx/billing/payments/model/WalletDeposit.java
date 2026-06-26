package projectlx.billing.payments.model;

import java.time.LocalDateTime;

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
import projectlx.billing.payments.utils.enums.WalletDepositPurpose;
import projectlx.billing.payments.utils.enums.WalletDepositStatus;
import projectlx.billing.payments.utils.enums.WalletReceiptEmailStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

@Entity
@Table(name = "wallet_deposit")
@Getter
@Setter
@ToString
public class WalletDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "notes", length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WalletDepositStatus status = WalletDepositStatus.PENDING;

    /** Whether this payment tops up the wallet or activates a subscription. */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private WalletDepositPurpose purpose = WalletDepositPurpose.WALLET_TOPUP;

    /** Target subscription package when {@code purpose = SUBSCRIPTION}. */
    @Column(name = "subscription_package_id")
    private Long subscriptionPackageId;

    @Column(name = "proof_document_id")
    private Long proofDocumentId;

    @Column(name = "gateway_provider", length = 50)
    private String gatewayProvider;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Best-effort outcome of dispatching the receipt email when this deposit was approved. */
    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_email_status", length = 20)
    private WalletReceiptEmailStatus receiptEmailStatus;

    @Column(name = "receipt_email_address", length = 255)
    private String receiptEmailAddress;

    @Column(name = "receipt_email_at")
    private LocalDateTime receiptEmailAt;

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
