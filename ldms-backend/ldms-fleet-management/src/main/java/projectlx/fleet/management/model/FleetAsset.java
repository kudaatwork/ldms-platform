package projectlx.fleet.management.model;

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
import projectlx.fleet.management.utils.enums.FleetAssetStatus;
import projectlx.fleet.management.utils.enums.FleetAssetType;
import projectlx.fleet.management.utils.enums.FleetContractScope;
import projectlx.fleet.management.utils.enums.FleetOwnershipType;
import projectlx.fleet.management.utils.enums.FleetRegistrationStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fleet_asset", indexes = {
        @Index(name = "idx_fleet_asset_org_status", columnList = "organization_id, entity_status"),
        @Index(name = "idx_fleet_asset_registration", columnList = "registration")
})
@Getter
@Setter
@ToString
public class FleetAsset implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private FleetAssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_type", nullable = false, length = 50)
    private FleetOwnershipType ownershipType = FleetOwnershipType.OWNED;

    @Column(name = "contracted_transporter_organization_id")
    private Long contractedTransporterOrganizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_scope", nullable = false, length = 50)
    private FleetContractScope contractScope = FleetContractScope.LONG_TERM;

    @Column(name = "job_reference", length = 100)
    private String jobReference;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "registration", nullable = false, length = 50)
    private String registration;

    @Column(name = "make_model", nullable = false, length = 200)
    private String makeModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FleetAssetStatus status = FleetAssetStatus.AVAILABLE;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "fleet_driver_id")
    private Long fleetDriverId;

    @Column(name = "utilization_pct", nullable = false, precision = 19, scale = 2)
    private BigDecimal utilizationPct = BigDecimal.ZERO;

    @Column(name = "max_speed_kmh", precision = 6, scale = 2)
    private BigDecimal maxSpeedKmh;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 50)
    private FleetRegistrationStatus registrationStatus = FleetRegistrationStatus.ACTIVE;

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
