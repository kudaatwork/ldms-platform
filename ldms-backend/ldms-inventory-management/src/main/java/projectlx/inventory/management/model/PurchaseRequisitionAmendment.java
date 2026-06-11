package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

/**
 * Purchase Requisition Amendment - Tracks changes to approved PRs.
 *
 * Approved PRs cannot be edited directly. Instead, amendments are created
 * to record changes in quantities, fulfillment methods, or closure reasons.
 * This maintains a complete audit trail.
 */
@Entity
@Table(name = "purchase_requisition_amendment", indexes = {
        @Index(name = "idx_pr_amendment_pr_id", columnList = "purchase_requisition_id"),
        @Index(name = "idx_pr_amendment_created", columnList = "created_at")
})
@Getter
@Setter
@ToString
public class PurchaseRequisitionAmendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_requisition_id", nullable = false)
    @ToString.Exclude
    private PurchaseRequisition purchaseRequisition;

    @Column(name = "amendment_number", nullable = false)
    private Integer amendmentNumber; // Sequential amendment number

    @Column(name = "amendment_type", nullable = false, length = 50)
    private String amendmentType; // e.g., "QUANTITY_CHANGE", "FULFILLMENT_METHOD_CHANGE", "CLOSURE"

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description; // What was changed and why

    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue; // JSON or text of old value

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue; // JSON or text of new value

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // Business reason for amendment

    @Column(name = "line_id")
    private Long lineId; // If amendment affects specific line

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 20)
    private EntityStatus entityStatus;

    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;
    }
}
