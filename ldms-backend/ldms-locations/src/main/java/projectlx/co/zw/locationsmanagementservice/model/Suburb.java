package projectlx.co.zw.locationsmanagementservice.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table
@Getter
@Setter
@ToString
public class Suburb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g., "Westgate"
    @Column(nullable = false)
    private String name;

    // Optional code, e.g., "WGT"
    @Column(length = 10)
    private String code;

    // Belongs to a district
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    // Optional: Link to coordinates (can be reused if locations are mapped independently)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_coordinates_id")
    private GeoCoordinates geoCoordinates;

    // Optional postal code
    private String postalCode;

    // Optional admin level (e.g., suburb = 3 if country=0, province=1, district=2)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrative_level_id")
    private AdministrativeLevel administrativeLevel;

    @OneToMany(mappedBy = "suburb", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<LocalizedName> localizedNames;

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
