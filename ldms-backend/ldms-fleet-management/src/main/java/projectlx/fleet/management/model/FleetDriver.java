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
import projectlx.fleet.management.utils.enums.DriverEmploymentType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fleet_driver", indexes = {
        @Index(name = "idx_fleet_driver_org_status", columnList = "organization_id, entity_status"),
        @Index(name = "idx_fleet_driver_user_id", columnList = "user_id")
})
@Getter
@Setter
@ToString
public class FleetDriver implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 50)
    private DriverEmploymentType employmentType = DriverEmploymentType.EMPLOYED;

    @Column(name = "user_id")
    private Long userId;

    // === PERSONAL DETAILS ===

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "license_number", nullable = false, length = 100)
    private String licenseNumber;

    @Column(name = "license_class", length = 50)
    private String licenseClass;

    @Column(name = "license_upload_id")
    private Long licenseUploadId;

    @Column(name = "license_back_upload_id")
    private Long licenseBackUploadId;

    // === IDENTITY DOCUMENTS ===

    @Column(name = "national_id_number", length = 100)
    private String nationalIdNumber;

    @Column(name = "national_id_expiry_date")
    private LocalDate nationalIdExpiryDate;

    @Column(name = "national_id_upload_id")
    private Long nationalIdUploadId;

    @Column(name = "national_id_back_upload_id")
    private Long nationalIdBackUploadId;

    @Column(name = "passport_number", length = 100)
    private String passportNumber;

    @Column(name = "passport_expiry_date")
    private LocalDate passportExpiryDate;

    @Column(name = "passport_upload_id")
    private Long passportUploadId;

    // === RESIDENTIAL ADDRESS ===

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "address_city", length = 100)
    private String addressCity;

    @Column(name = "address_province", length = 100)
    private String addressProvince;

    @Column(name = "address_postal_code", length = 30)
    private String addressPostalCode;

    @Column(name = "address_country", length = 100)
    private String addressCountry;

    // === MARKETPLACE ===

    /** When true the driver appears in the freelance marketplace. */
    @Column(name = "marketplace_visible", nullable = false)
    private Boolean marketplaceVisible = Boolean.FALSE;

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
