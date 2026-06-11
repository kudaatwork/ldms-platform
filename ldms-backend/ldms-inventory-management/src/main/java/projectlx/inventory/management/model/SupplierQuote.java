package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.model.PaymentTerm;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier_quote")
@Getter
@Setter
@ToString
public class SupplierQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_number", nullable = false, unique = true, length = 50)
    private String quoteNumber;

    @Column(name = "purchase_requisition_id", nullable = false)
    private Long purchaseRequisitionId;

    @Column(name = "supplier_organization_id", nullable = false)
    private Long supplierOrganizationId;

    @Column(name = "customer_organization_id", nullable = false)
    private Long customerOrganizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SupplierQuoteStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_source", nullable = false, length = 30)
    private SupplierQuoteSource quoteSource;

    @Column(name = "external_document_id")
    private Long externalDocumentId;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal", precision = 19, scale = 4, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_term", length = 50)
    private PaymentTerm paymentTerm;

    @Column(name = "delivery_terms", columnDefinition = "TEXT")
    private String deliveryTerms;

    @Column(name = "validity_until")
    private LocalDate validityUntil;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_by_user_id")
    private Long submittedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;

    @OneToMany(mappedBy = "supplierQuote", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<SupplierQuoteLine> lines = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (entityStatus == null) {
            entityStatus = EntityStatus.ACTIVE;
        }
        if (status == null) {
            status = SupplierQuoteStatus.DRAFT;
        }
        if (quoteSource == null) {
            quoteSource = SupplierQuoteSource.SYSTEM_GENERATED;
        }
    }

    @PreUpdate
    public void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    public void calculateTotals() {
        if (lines == null || lines.isEmpty()) {
            subtotal = BigDecimal.ZERO;
            taxAmount = BigDecimal.ZERO;
            totalAmount = BigDecimal.ZERO;
            return;
        }
        subtotal = lines.stream()
                .map(SupplierQuoteLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalAmount = subtotal.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
    }
}
