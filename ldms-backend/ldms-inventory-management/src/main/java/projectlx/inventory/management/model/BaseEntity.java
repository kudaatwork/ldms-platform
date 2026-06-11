package projectlx.inventory.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;

/**
 * Base entity class containing common fields for all entities in the system.
 * Provides audit trail, soft delete capability, and optimistic locking.
 */
@MappedSuperclass
@Getter
@Setter
@ToString
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false)
    private EntityStatus entityStatus;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Called before entity is persisted to database
     */
    @PrePersist
    protected void create() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.entityStatus == null) {
            this.entityStatus = EntityStatus.ACTIVE;
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }

    /**
     * Called before entity is updated in database
     */
    @PreUpdate
    protected void update() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Soft delete the entity by setting status to DELETED
     */
    public void softDelete() {
        this.entityStatus = EntityStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if entity is active (not soft deleted)
     */
    public boolean isActive() {
        return this.entityStatus == EntityStatus.ACTIVE;
    }

    /**
     * Check if entity is soft deleted
     */
    public boolean isDeleted() {
        return this.entityStatus == EntityStatus.DELETED;
    }

    /**
     * Check if entity is inactive/disabled
     */
    public boolean isInactive() {
        return this.entityStatus == EntityStatus.INACTIVE;
    }
}