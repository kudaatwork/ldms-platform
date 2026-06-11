package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_quote_line")
@Getter
@Setter
@ToString
public class SupplierQuoteLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_quote_id", nullable = false)
    @ToString.Exclude
    private SupplierQuote supplierQuote;

    @Column(name = "purchase_requisition_line_id", nullable = false)
    private Long purchaseRequisitionLineId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quoted_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal quotedQuantity;

    @Column(name = "unit_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (entityStatus == null) {
            entityStatus = EntityStatus.ACTIVE;
        }
        if (quotedQuantity != null && unitPrice != null) {
            lineTotal = quotedQuantity.multiply(unitPrice);
        }
    }

    @PreUpdate
    public void onUpdate() {
        modifiedAt = LocalDateTime.now();
        if (quotedQuantity != null && unitPrice != null) {
            lineTotal = quotedQuantity.multiply(unitPrice);
        }
    }
}
