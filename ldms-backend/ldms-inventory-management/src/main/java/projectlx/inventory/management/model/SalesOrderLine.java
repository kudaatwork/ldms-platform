package projectlx.inventory.management.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;

@Entity
@Table(name = "sales_order_line")
@Getter
@Setter
@ToString
public class SalesOrderLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    @ToString.Exclude
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @Column(name = "quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "fulfilled_quantity", precision = 19, scale = 4)
    private BigDecimal fulfilledQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure")
    private UnitOfMeasure unitOfMeasure;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    public void create() {
        super.create();
        if (fulfilledQuantity == null) fulfilledQuantity = BigDecimal.ZERO;
        if (totalPrice == null && quantity != null && unitPrice != null) {
            totalPrice = quantity.multiply(unitPrice);
        }
    }

    public void update() {
        super.update();
        if (quantity != null && unitPrice != null) {
            totalPrice = quantity.multiply(unitPrice);
        }
    }

    // Business method to check if line is fully fulfilled
    public boolean isFullyFulfilled() {
        return fulfilledQuantity != null && fulfilledQuantity.compareTo(quantity) >= 0;
    }

    // Business method to get remaining quantity
    public BigDecimal getRemainingQuantity() {
        if (fulfilledQuantity == null) return quantity;
        return quantity.subtract(fulfilledQuantity);
    }
}