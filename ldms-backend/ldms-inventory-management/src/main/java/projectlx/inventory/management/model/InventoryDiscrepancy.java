package projectlx.inventory.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_discrepancy")
@Data
public class InventoryDiscrepancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "warehouse_location_id")
    private Long warehouseLocationId;

    @Column(name = "expected_quantity", nullable = false)
    private BigDecimal expectedStock;

    @Column(name = "actual_quantity", nullable = false)
    private BigDecimal actualStock;

    @Column(name = "variance", nullable = false)
    private BigDecimal variance;

    @Column(name = "discovered_at")
    private LocalDateTime discoveredAt;
}
