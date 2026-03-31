package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@ToString
public class GeoCoordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 10, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6, nullable = false)
    private BigDecimal longitude;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.entityStatus = EntityStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
