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
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fleet.management.utils.enums.TrackingDeviceType;
import projectlx.fleet.management.utils.enums.TrackingInstallStatus;
import projectlx.fleet.management.utils.enums.TrackingIntegrationProvider;

import java.time.LocalDateTime;

@Entity
@Table(name = "fleet_tracking_device", indexes = {
        @Index(name = "idx_ftd_org", columnList = "organization_id, entity_status"),
        @Index(name = "idx_ftd_asset", columnList = "fleet_asset_id, entity_status"),
        @Index(name = "idx_ftd_ingest", columnList = "ingest_key")
})
@Getter
@Setter
@ToString
public class FleetTrackingDevice implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === OWNERSHIP ===
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "fleet_asset_id")
    private Long fleetAssetId;

    @Column(name = "fleet_driver_id")
    private Long fleetDriverId;

    @Column(name = "linked_user_id")
    private Long linkedUserId;

    // === DEVICE IDENTITY ===
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 50)
    private TrackingDeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "install_status", nullable = false, length = 50)
    private TrackingInstallStatus installStatus = TrackingInstallStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_provider", nullable = false, length = 50)
    private TrackingIntegrationProvider integrationProvider = TrackingIntegrationProvider.LDMS_MOBILE;

    @Column(name = "device_label", nullable = false, length = 150)
    private String deviceLabel;

    @Column(name = "device_serial", length = 100)
    private String deviceSerial;

    @Column(name = "external_device_id", length = 150)
    private String externalDeviceId;

    @Column(name = "ingest_key", nullable = false, unique = true, length = 64)
    private String ingestKey;

    // === TELEMETRY CAPABILITIES ===
    @Column(name = "tracks_gps", nullable = false)
    private boolean tracksGps = true;

    @Column(name = "tracks_fuel", nullable = false)
    private boolean tracksFuel = false;

    @Column(name = "mqtt_topic", length = 255)
    private String mqttTopic;

    // === LIFECYCLE ===
    @Column(name = "installed_at")
    private LocalDateTime installedAt;

    @Column(name = "last_telemetry_at")
    private LocalDateTime lastTelemetryAt;

    @Column(name = "notes", length = 500)
    private String notes;

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
