package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_route_stop", indexes = {
        @Index(name = "idx_route_stop_context", columnList = "context_type, context_id, stop_sequence"),
        @Index(name = "idx_route_stop_org",     columnList = "organization_id")
})
@Getter
@Setter
@ToString
public class LogisticsRouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === OWNERSHIP ===

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // === CONTEXT (the owning business document) ===

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 50)
    private RouteStopContextType contextType;

    @Column(name = "context_id", nullable = false)
    private Long contextId;

    // === STOP DEFINITION ===

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", nullable = false, length = 50)
    private RouteStopType stopType;

    @Column(name = "warehouse_location_id")
    private Long warehouseLocationId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "location_label", length = 200)
    private String locationLabel;

    // === AUDIT ===

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @PrePersist
    protected void onCreate() {
        if (entityStatus == null) {
            entityStatus = EntityStatus.ACTIVE;
        }
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
