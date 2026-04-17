package projectlx.co.zw.fileuploadservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.enums.StorageProvider;
import projectlx.co.zw.shared_library.utils.enums.VerificationMethod;
import projectlx.co.zw.shared_library.utils.enums.VerificationSource;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "file_upload",
        indexes = {
                @Index(name = "idx_file_upload_owner", columnList = "owner_type, owner_id, entity_status"),
                @Index(name = "idx_file_upload_stored", columnList = "stored_file_name"),
                @Index(name = "idx_file_upload_original", columnList = "original_file_name, entity_status")
        }
)
@Getter
@Setter
@ToString
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_file_name", nullable = false, length = 500)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 500)
    private String storedFileName;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "file_size_in_bytes", nullable = false)
    private Long fileSizeInBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 50)
    private StorageProvider storageProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 50)
    private OwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 50)
    private FileType fileType;

    @Column(name = "file_hash", length = 128)
    private String fileHash;

    @Column(name = "auto_verified", nullable = false)
    private boolean autoVerified;

    @Column(name = "auto_verification_notes", length = 500)
    private String autoVerificationNotes;

    @Column(name = "auto_verified_at")
    private LocalDateTime autoVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_verification_method", length = 50)
    private VerificationMethod autoVerificationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_verification_source", length = 50)
    private VerificationSource autoVerificationSource;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;

    @PrePersist
    public void prePersist() {
        if (entityStatus == null) {
            entityStatus = EntityStatus.ACTIVE;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (createdBy == null || createdBy.isBlank()) {
            createdBy = "SYSTEM";
        }
    }

    @PreUpdate
    public void preUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
