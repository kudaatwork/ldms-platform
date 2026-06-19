package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cross_dock_dispatch", indexes = {
        @Index(name = "idx_cross_dock_org",          columnList = "organization_id"),
        @Index(name = "idx_cross_dock_ext_dispatch",  columnList = "external_dispatch_id"),
        @Index(name = "idx_cross_dock_status",        columnList = "status, entity_status")
})
@Getter
@Setter
@ToString
public class CrossDockDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === OWNERSHIP ===

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // === DISPATCH PAYLOAD ===

    @Column(name = "external_dispatch_id", nullable = false, length = 100)
    private String externalDispatchId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "external_product_id", length = 100)
    private String externalProductId;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "from_location_label", length = 200)
    private String fromLocationLabel;

    @Column(name = "to_location_label", length = 200)
    private String toLocationLabel;

    @Column(name = "customer_reference", length = 200)
    private String customerReference;

    @Column(name = "en_route_depot_labels", columnDefinition = "TEXT")
    private String enRouteDepotLabels;

    // === STATUS & LINKS ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private CrossDockDispatchStatus status = CrossDockDispatchStatus.PENDING;

    @Column(name = "shipment_id")
    private Long shipmentId;

    @Column(name = "shipment_number", length = 100)
    private String shipmentNumber;

    @Column(name = "integration_credential_id", nullable = false)
    private Long integrationCredentialId;

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
        if (status == null) {
            status = CrossDockDispatchStatus.PENDING;
        }
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
