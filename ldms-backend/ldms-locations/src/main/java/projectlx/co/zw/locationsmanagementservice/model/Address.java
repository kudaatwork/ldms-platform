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

    // Suburb level
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suburb_id")
    private Suburb suburb;

    // Optional: Coordinates specific to this address
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
