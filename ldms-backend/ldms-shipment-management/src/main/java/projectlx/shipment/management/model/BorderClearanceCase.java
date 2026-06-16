package projectlx.shipment.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.shipment.management.utils.enums.BorderClearanceStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "border_clearance_case", indexes = {
        @Index(name = "idx_bcc_org_id", columnList = "organization_id"),
        @Index(name = "idx_bcc_shipment_id", columnList = "shipment_id"),
        @Index(name = "idx_bcc_status", columnList = "status, entity_status"),
        @Index(name = "idx_bcc_transfer_id", columnList = "inventory_transfer_id")
})
@Getter
@Setter
@ToString
public class BorderClearanceCase implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number", nullable = false, unique = true, length = 50)
    private String caseNumber;

    // === ORGANISATION & SOURCE ===

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(name = "inventory_transfer_id")
    private Long inventoryTransferId;

    @Column(name = "sales_order_id")
    private Long salesOrderId;

    @Column(name = "trip_id")
    private Long tripId;

    // === CLEARANCE DETAILS ===

    @Column(name = "border_name", length = 200)
    private String borderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BorderClearanceStatus status = BorderClearanceStatus.AWAITING_DOCUMENTS;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;

    @Column(name = "cleared_by", length = 150)
    private String clearedBy;

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
