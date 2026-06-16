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
import projectlx.shipment.management.utils.enums.BorderClearanceDocumentType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "border_clearance_document", indexes = {
        @Index(name = "idx_bcd_case_id", columnList = "case_id"),
        @Index(name = "idx_bcd_document_type", columnList = "document_type, entity_status")
})
@Getter
@Setter
@ToString
public class BorderClearanceDocument implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === CASE REFERENCE ===

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    // === DOCUMENT DETAILS ===

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private BorderClearanceDocumentType documentType;

    @Column(name = "file_upload_id", nullable = false)
    private Long fileUploadId;

    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    @Column(name = "description", length = 500)
    private String description;

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
