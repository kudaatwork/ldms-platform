package projectlx.user.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "demo_requisition")
@Getter
@Setter
public class DemoRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requisition_number", nullable = false, length = 32, unique = true)
    private String requisitionNumber;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "demo_request", nullable = false, columnDefinition = "TEXT")
    private String demoRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DemoRequisitionStatus status;

    @Column(name = "assigned_handler_username", length = 150)
    private String assignedHandlerUsername;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "contacted_at")
    private LocalDateTime contactedAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
        if (status == null) {
            status = DemoRequisitionStatus.NEW;
        }
    }

    @PreUpdate
    public void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
