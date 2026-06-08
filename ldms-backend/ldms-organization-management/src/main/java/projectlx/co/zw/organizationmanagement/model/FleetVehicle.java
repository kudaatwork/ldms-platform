package projectlx.co.zw.organizationmanagement.model;

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
import projectlx.co.zw.organizationmanagement.utils.enums.FleetVehicleOwnershipType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization_fleet_vehicle", indexes = {
        @Index(name = "idx_fleet_vehicle_org_status", columnList = "organization_id, entity_status"),
        @Index(name = "idx_fleet_vehicle_registration", columnList = "registration")
})
@Getter
@Setter
@ToString
public class FleetVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "ownership_type", nullable = false, length = 50)
    private String ownershipType = FleetVehicleOwnershipType.OWNED.name();

    @Column(name = "contracted_transporter_organization_id")
    private Long contractedTransporterOrganizationId;

    @Column(name = "registration", nullable = false, length = 50)
    private String registration;

    @Column(name = "make_model", nullable = false, length = 200)
    private String makeModel;

    // rig, van, tanker, flatbed
    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    // available, on_road, yard, maintenance
    @Column(name = "status", nullable = false, length = 50)
    private String status = "available";

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "utilization_pct", nullable = false, precision = 19, scale = 2)
    private BigDecimal utilizationPct = BigDecimal.ZERO;

    @Column(name = "last_trip_at")
    private LocalDateTime lastTripAt;

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
