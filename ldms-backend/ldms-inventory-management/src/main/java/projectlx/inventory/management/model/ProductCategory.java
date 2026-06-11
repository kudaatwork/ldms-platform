package projectlx.inventory.management.model;

import jakarta.persistence.*;
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
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "category")
    @ToString.Exclude
    private List<ProductSubCategory> subcategories;

    @OneToMany(mappedBy = "category")
    @ToString.Exclude
    private List<Product> products;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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