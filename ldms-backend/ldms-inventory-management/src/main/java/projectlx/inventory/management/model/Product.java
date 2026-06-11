package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(indexes = {
        @Index(name = "ux_product_product_code", columnList = "product_code", unique = true)
})
@Getter
@Setter
@ToString
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // External reference to supplier service
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false)
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "barcode")
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private ProductSubCategory subcategory;

    @Column(name = "manufacturer")
    private String manufacturer;

    // External reference to file-upload service
    @Column(name = "image_id")
    private Long imageId;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ProductDocument> documents;

    @OneToMany(mappedBy = "product")
    @ToString.Exclude
    private List<InventoryItem> inventoryItems;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @PreUpdate
    public void update() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void create() {
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;
    }
}
