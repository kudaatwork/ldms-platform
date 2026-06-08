package projectlx.co.zw.fleetmanagement.model;

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
import projectlx.co.zw.fleetmanagement.utils.enums.ComplianceStatus;
import projectlx.co.zw.fleetmanagement.utils.enums.ComplianceSubjectType;
import projectlx.co.zw.fleetmanagement.utils.enums.ComplianceType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "fleet_compliance_record", indexes = {
        @Index(name = "idx_fleet_compliance_org_status", columnList = "organization_id, entity_status"),
        @Index(name = "idx_fleet_compliance_subject", columnList = "subject_type, subject_id"),
        @Index(name = "idx_fleet_compliance_expires", columnList = "expires_at, status")
})
@Getter
@Setter
@ToString
public class FleetComplianceRecord implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 50)
    private ComplianceSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_type", nullable = false, length = 50)
    private ComplianceType complianceType;

    @Column(name = "file_upload_id")
    private Long fileUploadId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ComplianceStatus status = ComplianceStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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
