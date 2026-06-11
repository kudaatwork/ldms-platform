package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "procurement_approval_review")
@Getter
@Setter
public class ProcurementApprovalReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private ProcurementApprovalDocumentType documentType;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "stage_number", nullable = false)
    private Integer stageNumber;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision;

    @Column(name = "reviewed_by_user_id", nullable = false)
    private Long reviewedByUserId;

    @Column(name = "reviewed_by_username", nullable = false, length = 150)
    private String reviewedByUsername;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

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
        if (reviewedAt == null) {
            reviewedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
