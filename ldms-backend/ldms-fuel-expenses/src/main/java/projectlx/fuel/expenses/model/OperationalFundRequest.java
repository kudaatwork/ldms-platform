package projectlx.fuel.expenses.model;

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
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fuel.expenses.utils.enums.FundRequestStatus;
import projectlx.fuel.expenses.utils.enums.FundRequestType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operational_fund_request", indexes = {
        @Index(name = "idx_ofr_trip_id",       columnList = "trip_id"),
        @Index(name = "idx_ofr_org_status",    columnList = "organization_id, entity_status"),
        @Index(name = "idx_ofr_driver_status", columnList = "fleet_driver_id, status"),
        @Index(name = "idx_ofr_status_entity", columnList = "status, entity_status")
})
@Getter
@Setter
@ToString
public class OperationalFundRequest implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === IDENTIFICATION ===

    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    // === TRIP CONTEXT ===

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "fleet_driver_id", nullable = false)
    private Long fleetDriverId;

    @Column(name = "fleet_asset_id")
    private Long fleetAssetId;

    // === REQUEST DETAILS ===

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 50)
    private FundRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FundRequestStatus status = FundRequestStatus.PENDING;

    @Column(name = "liters_requested", precision = 19, scale = 2)
    private BigDecimal litersRequested;

    @Column(name = "amount_requested", precision = 19, scale = 4)
    private BigDecimal amountRequested;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "driver_notes", length = 1000)
    private String driverNotes;

    // === DECISION ===

    @Column(name = "approved_liters", precision = 19, scale = 2)
    private BigDecimal approvedLiters;

    @Column(name = "approved_amount", precision = 19, scale = 4)
    private BigDecimal approvedAmount;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "decided_by", length = 150)
    private String decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    // === SOFT DELETE ===

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    // === AUDIT ===

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;
}
