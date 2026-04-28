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
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Official name e.g., "Zimbabwe"
    @Column(nullable = false, unique = true)
    private String name;

    // ISO 3166-1 codes
    @Column(length = 2, nullable = false, unique = true)
    private String isoAlpha2Code; // e.g., "ZW"

    @Column(length = 3, nullable = false, unique = true)
    private String isoAlpha3Code; // e.g., "ZWE"

    // International dial code e.g., "+263"
    @Column(nullable = false)
    private String dialCode;

    // Default timezone e.g., "Africa/Harare"
    @Column(nullable = false)
    private String timezone;

    // Optional: default currency code e.g., "USD"
    private String currencyCode;

    // Optional: Link to geolocation metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_coordinates_id")
    private GeoCoordinates geoCoordinates;

    // Optional: Localized names from LocalizedName table
    @OneToMany(mappedBy = "country", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<LocalizedName> localizedNames;

    // Optional: Country-level administrative levels (e.g., ADM1 = Province, ADM2 = District)
    @OneToMany(mappedBy = "country", fetch = FetchType.LAZY)
    private List<AdministrativeLevel> administrativeLevels;

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
