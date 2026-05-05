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
import lombok.ToString.Exclude;
import projectlx.co.zw.locationsmanagementservice.utils.enums.SettlementType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@ToString
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g., "123 Samora Machel Avenue"
    @Column(nullable = false)
    private String line1;

    // Optional: e.g., "Apartment 4B"
    private String line2;

    // Postal code or ZIP code
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type")
    private SettlementType settlementType;

    @Column(name = "external_source", length = 50)
    private String externalSource;

    @Column(name = "external_place_id", length = 255)
    private String externalPlaceId;

    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "suburb_id")
    private Suburb suburb;

    /** Denormalized city context for suburb settlements (optional). */
    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id")
    private Village village;

    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_location_node_id")
    private LocationNode villageLocationNode;

    // Optional: Coordinates specific to this address
    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geo_coordinates_id")
    private GeoCoordinates geoCoordinates;

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
