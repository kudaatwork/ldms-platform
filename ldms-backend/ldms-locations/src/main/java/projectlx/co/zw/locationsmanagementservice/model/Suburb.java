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
import lombok.ToString.Exclude;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    /** First-class city under the district (preferred; replaces {@link #cityLocationNode} over time). */
    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_location_node_id")
    private LocationNode cityLocationNode;

    // Optional: Link to coordinates (can be reused if locations are mapped independently)
    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_coordinates_id")
    private GeoCoordinates geoCoordinates;

    // Optional postal code
    private String postalCode;

    // Optional admin level (e.g., suburb = 3 if country=0, province=1, district=2)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "administrative_level_id")
    private AdministrativeLevel administrativeLevel;

    @Exclude
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
