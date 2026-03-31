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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "location_node", indexes = {
        @Index(name = "idx_location_node_parent", columnList = "parent_id"),
        @Index(name = "idx_location_node_type", columnList = "location_type"),
        @Index(name = "idx_location_node_status", columnList = "entity_status")
})
@Getter
@Setter
@ToString(exclude = {"parent", "children", "aliases"})
public class LocationNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 50)
    private LocationType locationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private LocationNode parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<LocationNode> children = new ArrayList<>();

    @Column(precision = 10, scale = 7)
    private java.math.BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private java.math.BigDecimal longitude;

    @Column(length = 100)
    private String timezone;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus = EntityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @OneToMany(mappedBy = "locationNode", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LocationAlias> aliases = new ArrayList<>();
}
