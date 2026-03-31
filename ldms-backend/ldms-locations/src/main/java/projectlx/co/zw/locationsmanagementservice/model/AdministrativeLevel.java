package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@ToString
public class AdministrativeLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g., "District", "Province", "Ward", "Constituency"
    @Column(nullable = false, unique = true)
    private String name;

    // e.g., "ADM2", "ADM1", etc. – could be useful for standardization
    private String code;

    // For ordering levels: 0 (Country), 1 (Province), 2 (District), etc.
    @Column(nullable = false)
    private Integer level;

    // Optional description for clarification
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.entityStatus = EntityStatus.ACTIVE;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
