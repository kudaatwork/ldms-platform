package projectlx.trip.tracking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_delivery_return_line", indexes = {
        @Index(name = "idx_return_line_workflow", columnList = "workflow_id, entity_status")
})
@Getter
@Setter
@ToString(exclude = "workflow")
public class TripDeliveryReturnLine implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private TripDeliveryWorkflow workflow;

    @Column(name = "product_name", nullable = false, length = 500)
    private String productName;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "recorded_by_role", length = 50)
    private String recordedByRole;

    // === AUDIT ===

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;
}
