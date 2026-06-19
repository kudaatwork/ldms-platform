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
import projectlx.fleet.management.utils.enums.DriverSignupRequestStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "fleet_driver_signup_request", indexes = {
        @Index(name = "idx_signup_status", columnList = "status, entity_status")
})
@Getter
@Setter
@ToString
public class FleetDriverSignupRequest implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === ORGANISATION RESOLUTION ===

    @Column(name = "organization_id")
    private Long organizationId;

    /**
     * {@code COMPANY} — applicant links to a specific transport company via companyCode.
     * {@code FREELANCE} — applicant enters the marketplace for any org to hire.
     */
    @Column(name = "signup_type", nullable = false, length = 50)
    private String signupType = "COMPANY";

    /** Nullable for FREELANCE requests. */
    @Column(name = "company_code", length = 50)
    private String companyCode;

    // === APPLICANT DETAILS ===

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Column(name = "license_number", nullable = false, length = 100)
    private String licenseNumber;

    @Column(name = "license_class", length = 50)
    private String licenseClass;

    @Column(name = "national_id_number", length = 100)
    private String nationalIdNumber;

    /** Client-generated id grouping staging document uploads before submit. */
    @Column(name = "staging_session_id")
    private Long stagingSessionId;

    @Column(name = "national_id_front_upload_id")
    private Long nationalIdFrontUploadId;

    @Column(name = "national_id_back_upload_id")
    private Long nationalIdBackUploadId;

    @Column(name = "license_front_upload_id")
    private Long licenseFrontUploadId;

    @Column(name = "license_back_upload_id")
    private Long licenseBackUploadId;

    // === STATUS ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DriverSignupRequestStatus status = DriverSignupRequestStatus.PENDING;

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
