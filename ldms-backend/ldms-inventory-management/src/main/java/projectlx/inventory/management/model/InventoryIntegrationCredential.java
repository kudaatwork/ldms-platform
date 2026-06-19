package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_integration_credential", indexes = {
        @Index(name = "idx_integration_cred_org",    columnList = "organization_id"),
        @Index(name = "idx_integration_cred_status", columnList = "status, entity_status")
})
@Getter
@Setter
@ToString
public class InventoryIntegrationCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === OWNERSHIP ===

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // === CREDENTIAL DETAILS ===

    @Column(name = "credential_label", nullable = false, length = 200)
    private String credentialLabel;

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "callback_grv_url", length = 500)
    private String callbackGrvUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private IntegrationCredentialStatus status = IntegrationCredentialStatus.ACTIVE;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // === AUDIT ===

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = IntegrationCredentialStatus.ACTIVE;
        }
        if (entityStatus == null) {
            entityStatus = EntityStatus.ACTIVE;
        }
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
